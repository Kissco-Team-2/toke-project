package com.toke.toke_project.web.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WordbookCardDto {
    private Long id;
    private String title;
    private String description;
    private List<String> tags; // 여러 개의 태그
}
