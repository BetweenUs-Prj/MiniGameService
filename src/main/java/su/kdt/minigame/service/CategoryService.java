package su.kdt.minigame.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import su.kdt.minigame.repository.QuizQuestionRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    
    private final QuizQuestionRepo quizQuestionRepo;

    public List<String> listCategories() {
        // 빈 리스트여도 200 OK로 내려주면 프론트는 폴백을 사용함.
        return quizQuestionRepo.findAllDistinctCategories();
    }
}