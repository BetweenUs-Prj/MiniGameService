package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final GameRepo gameRepo;
    private final QuizRoundRepo roundRepo;
    private final QuizAnswerRepository answerRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;
    private final PenaltyRepository penaltyRepository;
    private final GamePenaltyRepository gamePenaltyRepository;

    @Transactional
    public SessionResp createQuizSession(CreateSessionReq req, String userUid) {
        GameSession session = new GameSession(req.appointmentId(), GameSession.GameType.QUIZ, userUid);
        GameSession savedSession = gameRepo.save(session);
        return SessionResp.from(savedSession);
    }

    @Transactional
    public RoundResp startRound(Long sessionId, CreateRoundReq req) {
        GameSession session = findSession(sessionId);
        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }
        session.start();

        QuizQuestion question = findQuestion(req.questionId());
        QuizRound round = new QuizRound(session, question);
        QuizRound savedRound = roundRepo.save(round);

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

        QuizAnswer answer = new QuizAnswer(round, req.userUid(), req.answerText());

        boolean correct = isCorrect(round.getQuestion(), req.answerText());
        if (correct) {
            long responseTimeMs = ChronoUnit.MILLIS.between(round.getStartTime(), answer.getAnswerTime());
            answer.grade(true, responseTimeMs);
        } else {
            answer.grade(false, 0L);
        }
        answerRepo.save(answer);

        return new AnswerResp(answer.getId(), correct, null, false);
    }

    @Transactional
    public void endQuizGame(Long sessionId) {
        GameSession session = findSession(sessionId);
        assignQuizPenalty(session);
    }

    private void assignQuizPenalty(GameSession session) {
        // TODO: Get the list of all participants for this session
        List<String> userUids = List.of("user1", "user2"); // Placeholder

        Map<String, Long> userTotalTimes = new HashMap<>();
        for (String uid : userUids) {
            Long totalTime = answerRepo.findTotalCorrectResponseTimeByUser(session.getId(), uid);
            userTotalTimes.put(uid, totalTime != null ? totalTime : Long.MAX_VALUE);
        }

        String loserUid = Collections.max(userTotalTimes.entrySet(), Map.Entry.comparingByValue()).getKey();

        List<Penalty> allPenalties = penaltyRepository.findAll();
        if (allPenalties.isEmpty()) {
            throw new IllegalStateException("No penalties found in the database.");
        }
        Collections.shuffle(allPenalties);
        Penalty selectedPenalty = allPenalties.get(0);

        GamePenalty gamePenalty = new GamePenalty(session, loserUid, selectedPenalty);
        gamePenaltyRepository.save(gamePenalty);

        session.finishGame(loserUid, selectedPenalty.getDescription());
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