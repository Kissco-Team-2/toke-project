package com.toke.toke_project.web;

import com.toke.toke_project.security.CustomUserDetails;
import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.web.dto.GenerateRequest;
import com.toke.toke_project.web.dto.QuizView;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/quiz")
public class QuizPageController {

    private final QuizService quizService;

    public QuizPageController(QuizService quizService) {
        this.quizService = quizService;
    }

    /** 카테고리 화면 */
    @GetMapping("/category")
    public String category() {
        return "quiz/category"; // templates/quiz/category.html
    }

    /** 퀴즈 생성 → 생성된 quizId로 리다이렉트 */
    @PostMapping("/start")
    public String start(@ModelAttribute GenerateRequest req,
                        @AuthenticationPrincipal CustomUserDetails principal,
                        RedirectAttributes ra) {
        QuizView view = quizService.generateForUser(req);
        ra.addFlashAttribute("msg", "퀴즈가 생성되었습니다!");
        // ✅ 여기서 quizId 사용
        return "redirect:/quiz/" + view.quizId();
    }

    /** 퀴즈 시작 화면 */
    @GetMapping("/{quizId}")
    public String play(@PathVariable String quizId, Model model, RedirectAttributes ra) {
        try {
            QuizView view = quizService.getViewByUuid(quizId);
            model.addAttribute("quiz", view);
            return "quiz/start";
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.GONE) {
                ra.addFlashAttribute("msg",
                        ex.getReason() != null ? ex.getReason() : "퀴즈가 종료되었습니다.");
                return "redirect:/quiz/category";
            }
            throw ex;
        }
    }
    
    @PostMapping("/{quizId}/submit")
    public String submit(@PathVariable String quizId,
                         @AuthenticationPrincipal CustomUserDetails principal,
                         @RequestParam Map<String, String> params,
                         Model model,
                         RedirectAttributes ra) {

        // 폼 파라미터에서 answers[0]=0, answers[1]=2 ... 형태를 추출
        Map<Integer, Integer> answers = new HashMap<>();
        Pattern p = Pattern.compile("^answers\\[(\\d+)]$");

        for (Map.Entry<String,String> e : params.entrySet()) {
            Matcher m = p.matcher(e.getKey());
            if (m.matches()) {
                String v = e.getValue();
                if (v != null && !v.isBlank()) {
                    answers.put(Integer.parseInt(m.group(1)), Integer.parseInt(v));
                }
            }
        }

        // GradeRequest 만들어 서비스 호출
        com.toke.toke_project.web.dto.GradeRequest req =
                new com.toke.toke_project.web.dto.GradeRequest(answers);

        var userId = principal.getId();
        var grade = quizService.grade(quizId, req, userId); // GradeResponse

        model.addAttribute("result", grade); // result.html에서 사용
        return "quiz/result";                // templates/quiz/result.html
    }
}
