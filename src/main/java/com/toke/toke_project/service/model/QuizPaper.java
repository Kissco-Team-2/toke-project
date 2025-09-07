package com.toke.toke_project.service.model;

import java.util.List;

/** 서버 캐시에 저장되는 퀴즈 전체(정답 포함) */
public record QuizPaper(
        String quizId,
        String category,
        QuestionMode mode,
        List<QuizQuestion> questions
) {}