package com.toke.toke_project.web.dto;

import java.time.LocalDateTime;

public record QnaAccordionRow(
        Long id,
        String title,
        String status,          // OPEN / ANSWERED / CLOSED
        LocalDateTime createdAt,
        String content,         // Qna.content
        String answerContent    // 답변 본문 (없으면 null) - TODO 실제 소스 연결
) {}
