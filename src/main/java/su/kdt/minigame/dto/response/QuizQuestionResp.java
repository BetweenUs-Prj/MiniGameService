package su.kdt.minigame.dto.response;

import su.kdt.minigame.domain.QuizQuestion;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public record QuizQuestionResp(
    Long questionId,
    String questionText,
    String category,
    List<Option> options
) {
    public record Option(Long optionId, String optionText, boolean isCorrect) {
        public static Option from(su.kdt.minigame.domain.QuizQuestionOption option) {
            return new Option(option.getOptionId(), option.getOptionText(), option.isCorrect());
        }
    }

    public static QuizQuestionResp from(QuizQuestion question) {
        List<Option> options = question.getOptions() != null ? 
            question.getOptions().stream()
                .map(Option::from)
                .collect(Collectors.toList()) : 
            new ArrayList<>();
        
        return new QuizQuestionResp(
            question.getId(),
            question.getQuestionText(),
            question.getCategory(),
            options
        );
    }
}