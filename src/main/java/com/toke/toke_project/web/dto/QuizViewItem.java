package com.toke.toke_project.web.dto;

import java.util.List;

/** 클라이언트로 내려가는 1문항 (정답 제거) */
public record QuizViewItem(
        String prompt,
        List<String> options
) {}