package com.toke.toke_project.web.dto;

import java.util.List;

import com.toke.toke_project.service.model.QuestionMode;

/** 클라이언트로 내려가는 퀴즈 (정답 제거) */
public record QuizView(
        String quizId,
        String category,
        QuestionMode mode,
        List<QuizViewItem> items
) {}
