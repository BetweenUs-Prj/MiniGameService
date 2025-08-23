package su.kdt.minigame.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import su.kdt.minigame.domain.QuizQuestion;
import su.kdt.minigame.domain.QuizQuestionOption;
import su.kdt.minigame.domain.QuizRound;
import su.kdt.minigame.repository.QuizQuestionOptionRepo;
import su.kdt.minigame.repository.QuizQuestionRepo;
import su.kdt.minigame.repository.QuizRoundRepo;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewQuizService {
    private final QuizQuestionRepo questionRepo;
    private final QuizQuestionOptionRepo optionRepo;
    private final QuizRoundRepo roundRepo;

    @Transactional
    public StartRoundRes startRound(Long sessionId, String category) {
        // 1) 카테고리에서 랜덤 질문 고르기 (없으면 전체에서 랜덤)
        List<QuizQuestion> pool = questionRepo.findByCategory(category);
        if (pool.isEmpty()) pool = questionRepo.findByCategory(null);
        if (pool.isEmpty()) throw new IllegalStateException("질문 데이터가 없습니다. data.sql을 확인하세요.");

        QuizQuestion q = pool.get((int) (Math.random() * pool.size()));

        // 2) 라운드 저장
        QuizRound r = new QuizRound(sessionId, q);
        r = roundRepo.save(r);

        // 3) 선택지 조회
        var opts = optionRepo.findByQuestionId(q.getId()).stream()
                .map(o -> new OptionDto(o.getOptionId(), o.getOptionText()))
                .toList();

        return new StartRoundRes(r.getRoundId(), new QuestionDto(q.getId(), q.getQuestionText(), q.getCategory(), opts));
    }

    @Getter
    @AllArgsConstructor
    public static class OptionDto {
        private Long id;
        private String text;
    }

    @Getter
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;
        private String text;
        private String category;
        private List<OptionDto> options;
    }

    @Getter
    @AllArgsConstructor
    public static class StartRoundRes {
        private Long roundId;
        private QuestionDto question;
    }
}