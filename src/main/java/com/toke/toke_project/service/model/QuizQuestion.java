package com.toke.toke_project.service.model;

import java.util.List;

/** 서버 캐시에 저장되는 1문항 (정답 포함) */
public record QuizQuestion(
		Long quizId,
        String prompt,       // 질문 문구
        List<String> options,// 보기 4개
        int answerIndex      // 0~3
) {}
