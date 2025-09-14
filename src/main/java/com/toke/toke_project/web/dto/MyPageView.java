package com.toke.toke_project.web.dto;

import lombok.*;
import org.springframework.data.domain.Page;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyPageView {
    private MyPageForm form;                 // 내 정보 수정 탭
    private Page<QnaListRow> qnas;           // 내 문의내역
    private Page<WordbookCardDto> wordbooks; // 내 단어장
    private String activeTab;                // 현재 탭
}
