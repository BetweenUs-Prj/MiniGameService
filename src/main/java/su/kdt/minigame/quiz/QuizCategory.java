package su.kdt.minigame.quiz;

import lombok.Getter;

/**
 * 퀴즈 카테고리 Enum + 양방향 매핑
 */
@Getter
public enum QuizCategory {
    GENERAL("상식"), 
    ALCOHOL("술"), 
    SPORTS("스포츠"), 
    HISTORY("역사"), 
    FOOD("음식");

    private final String ko;

    QuizCategory(String ko) {
        this.ko = ko;
    }

    /**
     * 영어 또는 한국어 카테고리 문자열을 QuizCategory Enum으로 변환
     * @param v 카테고리 값 (영어 또는 한국어)
     * @return QuizCategory Enum
     * @throws IllegalArgumentException 알 수 없는 카테고리인 경우
     */
    public static QuizCategory fromAny(String v) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("category cannot be null or empty");
        }
        
        String s = v.trim();
        for (QuizCategory c : values()) {
            if (c.name().equalsIgnoreCase(s) || c.ko.equals(s)) {
                return c;
            }
        }
        throw new IllegalArgumentException("unknown category: " + v);
    }
}