package com.toke.toke_project.web.dto;

import java.time.LocalDateTime;

/**
 * QnA 목록 한 줄 표현 DTO
 * - 엔티티를 뷰로 직접 넘기지 않고, 필요한 필드만 담아 Lazy 예외를 방지
 */
public record QnaListRow(
        Long id,
        String category,          // WORD / QUIZ / WORDLIST / OTHER
        String categoryLabel,     // 한글 라벨
        String title,
        String authorDisplay,     // Users에서 뽑은 표시이름(nickname/username/email 순)
        String status,            // OPEN / ANSWERED / CLOSED
        String isSecret,          // 'Y' / 'N'
        LocalDateTime createdAt
) {}
