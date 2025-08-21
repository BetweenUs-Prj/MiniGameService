// src/main/java/su/kdt/minigame/service/QuizService.java
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

import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.*;

// ⬇⬇ 추가
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

    // ⬇⬇ 추가: 리포지토리 메서드 유무 상관없이 JPQL로 계산
    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SessionResp createQuizSession(CreateSessionReq req, String userUid) {
        final int DEFAULT_ROUNDS = 5;
        Integer totalRounds = (req.totalRounds() != null && req.totalRounds() > 0)
                ? req.totalRounds()
                : DEFAULT_ROUNDS;

        GameSession session = new GameSession(req.appointmentId(), GameSession.GameType.QUIZ, userUid, req.penaltyId(), totalRounds);
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
            long responseTimeMs = ChronoUnit.MILLIS.between(round.getStartTime(), answer.getAnswerTime());
            answer.grade(true, responseTimeMs);
        } else {
            answer.grade(false, 0L);
        }
        answerRepo.save(answer);

        // ⬇⬇ 변경: 리포지토리 메서드 의존 제거 (JPQL로 라운드 수 집계)
        long currentRoundCount = em.createQuery(
                "select count(r) from QuizRound r where r.session = :session", Long.class)
                .setParameter("session", session)
                .getSingleResult();

        if (session.getTotalRounds() != null && currentRoundCount >= session.getTotalRounds()) {
            assignQuizPenalty(session);
        }

        return new AnswerResp(answer.getId(), correct, null, false);
    }

    // ⬇⬇ 추가: 컨트롤러가 호출하는 강제 종료용 메서드
    @Transactional
    public void endQuizGame(Long sessionId) {
        GameSession session = findSession(sessionId);
        assignQuizPenalty(session);
    }

    private void assignQuizPenalty(GameSession session) {
        // TODO: 약속 참가자 목록 조회 로직
        List<String> userUids = List.of("user1", "user2"); // Placeholder

        List<UserScore> scores = new ArrayList<>();
        for (String uid : userUids) {
            // ⬇⬇ 변경: JPQL로 정답 수/합계 응답시간 계산 (리포지토리 메서드 없어도 동작)
            Long correctCount = em.createQuery(
                    "select count(a) from QuizAnswer a " +
                    "where a.round.session.id = :sid and a.userUid = :uid and a.isCorrect = true",
                    Long.class)
                    .setParameter("sid", session.getId())
                    .setParameter("uid", uid)
                    .getSingleResult();

            Long totalTime = em.createQuery(
                    "select coalesce(sum(a.responseTimeMs), 0) from QuizAnswer a " +
                    "where a.round.session.id = :sid and a.userUid = :uid and a.isCorrect = true",
                    Long.class)
                    .setParameter("sid", session.getId())
                    .setParameter("uid", uid)
                    .getSingleResult();

            scores.add(new UserScore(
                    uid,
                    correctCount != null ? correctCount : 0L,
                    totalTime != null ? totalTime : 0L
            ));
        }

        // 정답 수 ↑, 동률이면 총 응답시간 ↓가 유리 → 패자 선정은 오름차순 정렬 후 첫 번째
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
