package com.toke.toke_project.web.dto;
//DTO (화면 ↔ 서버 간 전달 + 검증)
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;

@Getter @Setter
public class WordForm {
    private Long id;                          // 수정 시 사용

    @NotBlank @Size(max = 300)
    private String japaneseWord;

    @Size(max = 300)
    private String readingKana;

    @NotBlank @Size(max = 300)
    private String koreanMeaning;

    @Size(max = 200)
    private String category;

    @Size(max = 1000)
    private String exampleSentenceJp;
}

