package com.toke.toke_project.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.web.dto.GenerateRequest;
import com.toke.toke_project.web.dto.QuizView;

@RestController
@RequestMapping("/admin/quiz")
public class AdminQuizController {
	private final QuizService quizService;
	
	public AdminQuizController(QuizService quizService) {
		this.quizService=quizService;
	}
	
	 /** 관리자: 퀴즈 생성(정답 없는 뷰 + quizId 반환) */
    @PostMapping("/generate")
    public QuizView generate(@RequestBody GenerateRequest req) {
        return quizService.generate(req);
    }
    
}
