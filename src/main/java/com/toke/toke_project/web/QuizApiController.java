package com.toke.toke_project.web;

import com.toke.toke_project.security.CustomUserDetails;
import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.web.dto.GradeRequest;
import com.toke.toke_project.web.dto.GradeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizApiController {

    private final QuizService quizService;

    public QuizApiController(QuizService quizService) {
        this.quizService = quizService;
    }

    /** 채점(JSON) */
    @PostMapping("/{quizUuid}/grade")
    public GradeResponse grade(@PathVariable String quizUuid,
                               @RequestBody GradeRequest req,
                               @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getId();
        return quizService.grade(quizUuid, req, userId);
    }
}
