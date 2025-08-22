package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.*;
import su.kdt.minigame.dto.request.CreateRoundReq;
import su.kdt.minigame.dto.request.CreateSessionReq;
import su.kdt.minigame.dto.request.SubmitAnswerReq;
import su.kdt.minigame.dto.response.AnswerResp;
import su.kdt.minigame.dto.response.QuizQuestionResp;
import su.kdt.minigame.dto.response.RoundResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.*;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private record UserScore(String userUid, long correctAnswers, long totalTime) {}

    private final GameRepo gameRepo;
    private final QuizRoundRepo roundRepo;
    private final QuizAnswerRepository answerRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;
    private final GamePenaltyRepository gamePenaltyRepository;
    private final PenaltyRepository penaltyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public SessionResp createQuizSession(CreateSessionReq req, String userUid, Penalty selectedPenalty) {
        final int DEFAULT_ROUNDS = 5;
        Integer totalRounds = (req.totalRounds() != null && req.totalRounds() > 0)
                ? req.totalRounds()
                : DEFAULT_ROUNDS;

        GameSession session = new GameSession(req.appointmentId(), GameSession.GameType.QUIZ, userUid, selectedPenalty.getId(), totalRounds);
        GameSession savedSession = gameRepo.save(session);
        return SessionResp.from(savedSession);
    }

    @Transactional
    public RoundResp startRound(Long sessionId, CreateRoundReq req) {
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }

        Optional<QuizRound> latestRoundOpt = roundRepo.findTopBySessionOrderByIdDesc(session);
        if (latestRoundOpt.isPresent() && latestRoundOpt.get().getStatus() != QuizRound.Status.COMPLETED) {
            throw new IllegalStateException("아직 이전 라운드가 끝나지 않았습니다. 모든 참여자가 답변을 제출해야 합니다.");
        }

        session.start();

        QuizQuestion question = findQuestion(req.questionId());
        QuizRound round = new QuizRound(session, question);
        QuizRound savedRound = roundRepo.save(round);

        String destination = "/topic/game/" + sessionId;
        Map<String, Object> messagePayload = Map.of(
                "type", "NEW_ROUND_STARTED",
                "roundId", savedRound.getId(),
                "question", QuizQuestionResp.from(savedRound.getQuestion())
        );
        messagingTemplate.convertAndSend(destination, messagePayload);

        return RoundResp.from(savedRound);
    }

    @Transactional(readOnly = true)
    public Page<QuizQuestionResp> getQuestions(Long placeId, String category, Pageable pageable) {
        Page<QuizQuestion> questions = questionRepo.search(placeId, category, pageable);
        return questions.map(QuizQuestionResp::from);
    }

    @Transactional
    public AnswerResp submitAnswer(Long roundId, SubmitAnswerReq req) {
        QuizRound round = roundRepo.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));
        GameSession session = round.getSession();

        QuizAnswer answer = new QuizAnswer(round, req.userUid(), req.answerText());

        boolean correct = isCorrect(round.getQuestion(), req.answerText());
        if (correct) {
            long responseTimeMs = ChronoUnit.MILLIS.between(round.getStartTime().toInstant(ZoneOffset.UTC), answer.getAnswerTime());
            answer.grade(true, responseTimeMs);
        } else {
            answer.grade(false, 0L);
        }
        answerRepo.save(answer);

        int totalPlayers = 2; // Placeholder
        long answeredPlayers = answerRepo.countDistinctUserUidsByRound(round);

        if (answeredPlayers >= totalPlayers) {
            round.complete();
        }

        long currentRoundCount = roundRepo.countBySession(session);
        if (session.getTotalRounds() != null && currentRoundCount >= session.getTotalRounds()) {
            if (round.getStatus() == QuizRound.Status.COMPLETED) {
                assignQuizPenalty(session);
            }
        }
        
        return new AnswerResp(answer.getId(), correct, null, false);
    }

    private void assignQuizPenalty(GameSession session) {
        List<String> userUids = List.of("user1", "user2"); // Placeholder

        List<UserScore> scores = new ArrayList<>();
        for (String uid : userUids) {
            Long correctCount = answerRepo.countCorrectAnswersByUser(session.getId(), uid);
            Long totalTime = answerRepo.findTotalCorrectResponseTimeByUser(session.getId(), uid);
            scores.add(new UserScore(
                    uid,
                    correctCount != null ? correctCount : 0L,
                    totalTime != null ? totalTime : 0L
            ));
        }

        scores.sort(Comparator
                .comparing(UserScore::correctAnswers)
                .thenComparing(UserScore::totalTime, Comparator.reverseOrder()));

        String loserUid = scores.get(0).userUid();

        Long penaltyId = session.getSelectedPenaltyId();
        Penalty selectedPenalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new IllegalStateException("Selected penalty not found in DB: " + penaltyId));

        GamePenalty gamePenalty = new GamePenalty(session, loserUid, selectedPenalty);
        gamePenaltyRepository.save(gamePenalty);

        session.finish(selectedPenalty.getDescription());

        String destination = "/topic/game/" + session.getId();
        Map<String, Object> messagePayload = Map.of(
                "type", "GAME_FINISHED",
                "loserUid", loserUid,
                "penalty", selectedPenalty.getDescription()
        );
        messagingTemplate.convertAndSend(destination, messagePayload);
    }

    private GameSession findSession(Long sessionId) {
        return gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    private QuizQuestion findQuestion(Long questionId) {
        if (questionId == null) {
            long count = questionRepo.count();
            if (count == 0) throw new IllegalStateException("No quiz questions available");
            int randomIdx = new Random().nextInt((int) count);
            Page<QuizQuestion> page = questionRepo.findAll(PageRequest.of(randomIdx, 1));
            return page.getContent().get(0);
        }
        return questionRepo.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
    }

    private boolean isCorrect(QuizQuestion question, String answerText) {
        String normalizedAnswer = normalize(answerText);
        List<QuizQuestionOption> options = optionRepo.findByQuestion(question);

        return options.stream()
                .filter(QuizQuestionOption::isCorrect)
                .map(o -> normalize(o.getOptionText()))
                .anyMatch(normalizedAnswer::equals);
    }
    
    private String normalize(String s) {
        if (s == null) return "";
        s = s.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return Normalizer.normalize(s, Normalizer.Form.NFKC);
    }
}