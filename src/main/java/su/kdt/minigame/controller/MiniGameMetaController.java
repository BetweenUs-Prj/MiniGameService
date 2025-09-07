package su.kdt.minigame.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import su.kdt.minigame.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/mini-games")
@RequiredArgsConstructor
public class MiniGameMetaController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(categoryService.listCategories());
    }
}