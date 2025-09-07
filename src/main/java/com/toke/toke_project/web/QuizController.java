package com.toke.toke_project.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toke.toke_project.security.CustomUserDetails;
import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.web.dto.GradeRequest;
import com.toke.toke_project.web.dto.GradeResponse;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;
    public QuizController(QuizService quizService) { this.quizService = quizService; }

    /** 사용자: 채점 */
    @PostMapping("/{quizUuid}/grade")
    public GradeResponse grade(@PathVariable String quizUuid,
                               @RequestBody GradeRequest req,
                               @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getId();
        return quizService.grade(quizUuid, req, userId);
    }

}