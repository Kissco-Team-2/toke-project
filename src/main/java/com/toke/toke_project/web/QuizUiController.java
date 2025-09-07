package com.toke.toke_project.web;

import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.service.model.QuestionMode;
import com.toke.toke_project.web.dto.GenerateRequest;
import com.toke.toke_project.web.dto.GradeRequest;
import com.toke.toke_project.web.dto.GradeResponse;
import com.toke.toke_project.web.dto.QuizView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.toke.toke_project.security.CustomUserDetails;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizUiController {

    private final QuizService quizService;

    // 카테고리 선택 화면
    @GetMapping("/category")
    public String category() {
        return "quiz/category";
    }

    // 퀴즈 시작 (카테고리 + 유형 선택 → 문제 10개 생성)
    @PostMapping("/start")
    public String start(@RequestParam String category,
                        @RequestParam(defaultValue = "JP_TO_KR") QuestionMode mode,
                        Model model) {

        QuizView view = quizService.generateForUser(new GenerateRequest(category, mode, 10));
        model.addAttribute("quiz", view); // quiz.quizUuid, quiz.items(List<QuizViewItem>)
        return "quiz/start";              // 👉 quiz/start.html 렌더링
    }

    // 결과 화면 (제출)
    @PostMapping("/{uuid}/submit")
    public String submit(@PathVariable("uuid") String uuid,
                         @RequestParam Map<String, String> form,   // 전체 폼 데이터
                         @AuthenticationPrincipal CustomUserDetails principal,
                         Model model) {

        // "answers[0]" → index=0, value=2 이런 식으로 변환
        Map<Integer, Integer> fixed = new HashMap<>();
        form.forEach((k, v) -> {
            if (k.startsWith("answers[") && k.endsWith("]")) {
                try {
                    int idx = Integer.parseInt(k.substring(8, k.length() - 1));
                    int choice = Integer.parseInt(v); // 0..3
                    fixed.put(idx, choice);
                } catch (NumberFormatException ignore) {}
            }
        });

        GradeRequest req = new GradeRequest(fixed);
        GradeResponse res = quizService.grade(uuid, req, principal.getId());

        model.addAttribute("result", res);
        model.addAttribute("uuid", uuid);
        return "quiz/result"; // 👉 quiz/result.html 렌더링
    }
}
