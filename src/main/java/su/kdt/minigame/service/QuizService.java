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
import su.kdt.minigame.dto.response.QuizQuestionResp;
import su.kdt.minigame.dto.response.AnswerResp;
import su.kdt.minigame.dto.response.RoundResp;
import su.kdt.minigame.dto.response.SessionResp;
import su.kdt.minigame.repository.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final GameRepo gameRepo;
    private final QuizRoundRepo roundRepo;
    private final QuizAnswerRepo answerRepo;
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;

    @Transactional
    public SessionResp createQuizSession(CreateSessionReq req) {
        GameSession session = new GameSession();
        session.setAppointmentId(req.appointmentId());
        session.setGameType(GameSession.GameType.QUIZ);
        gameRepo.save(session);

        return new SessionResp(
                session.getId(),
                session.getAppointmentId(),
                session.getGameType().name(),
                session.getStatus().name(),
                session.getStartTime(),
                session.getEndTime()
        );
    }

    @Transactional
    public RoundResp startRound(Long sessionId, CreateRoundReq req) {
        GameSession session = gameRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getGameType() != GameSession.GameType.QUIZ) {
            throw new IllegalStateException("Not a quiz session");
        }
        
        session.start();

        QuizQuestion question = findQuestion(req.questionId());

        QuizRound round = new QuizRound();
        round.setSession(session);
        round.setQuestion(question);
        round.setStartTime(LocalDateTime.now());
        roundRepo.save(round);

        return new RoundResp(round.getId(), sessionId, round.getStartTime());
    }

    @Transactional(readOnly = true)
    public Page<QuizQuestionResp> getQuestions(Long placeId, String category, Pageable pageable) {
        Page<QuizQuestion> questions = questionRepo.search(placeId, category, pageable);
        return questions.map(QuizQuestionResp::from);
    }

    @Transactional
    public AnswerResp submitAnswer(Long roundId, SubmitAnswerReq req) {
        QuizRound round = roundRepo.findByIdWithLock(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        boolean correct = isCorrect(round.getQuestion(), req.answerText());
        
        // 라운드가 이미 종료되었는지 먼저 확인합니다.
        boolean wasClosed = round.isClosed();

        // 정답이고, 라운드가 아직 열려 있었다면 승자를 결정합니다.
        if (correct && !wasClosed) {
            round.decideWinner(req.userId());
            // roundRepo.save(round); // 변경 감지(Dirty Checking)로 인해 명시적 save 호출은 선택사항
        }
        
        // 답변 기록은 항상 저장합니다.
        QuizAnswer answer = saveAnswer(round, req, correct);

        return new AnswerResp(answer.getId(), correct, round.getWinnerUserId(), round.isClosed());
    }

    private QuizQuestion findQuestion(Long questionId) {
        if (questionId == null) {
            Page<QuizQuestion> page = questionRepo.findAll(PageRequest.of(0, 1));
            if (page.isEmpty()) throw new IllegalStateException("No quiz questions available");
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

    private QuizAnswer saveAnswer(QuizRound round, SubmitAnswerReq req, boolean correct) {
        QuizAnswer answer = new QuizAnswer();
        answer.setRound(round);
        answer.setUserId(req.userId());
        answer.setAnswerText(req.answerText());
        answer.setIsCorrect(correct);

        LocalDateTime ldt = req.answerTime() != null ? req.answerTime() : LocalDateTime.now();
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        answer.setAnswerTime(instant);

        return answerRepo.save(answer);
    }
}