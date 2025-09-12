package com.toke.toke_project.web.dto;

import java.time.LocalDateTime;

/**
 * QnA 상세 화면 DTO
 */
public record QnaDetailView(
        Long id,
        String category,
        String categoryLabel,     // 한글 라벨
        String title,
        String content,
        String status,
        String isSecret,
        LocalDateTime createdAt,

        // 작성자 / 답변자 표시 이름
        String authorDisplay

        
) {}
