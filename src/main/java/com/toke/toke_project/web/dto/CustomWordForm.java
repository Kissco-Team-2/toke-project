package com.toke.toke_project.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CustomWordForm {

    @NotBlank(message = "일본어 표현은 필수 입력입니다.")
    private String jp;

    private String kana;

    @NotBlank(message = "한국어 뜻은 필수 입력입니다.")
    private String kr;

    private String ex;
}
