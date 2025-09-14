package com.toke.toke_project.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MyPageForm {

    @NotBlank @Email
    private String email;

    // 한글/영문/숫자 1~6자
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9가-힣]{1,6}$",
             message = "닉네임은 한글/영문/숫자 1~6자만 가능합니다.")
    private String nickname;

    // 하이픈 없이 숫자만 9~15 자리 (원하면 국내 패턴으로 바꿔도 됨)
    @NotBlank
    @Pattern(regexp = "^[0-9]{9,15}$",
             message = "전화번호는 숫자만 입력하세요.")
    private String phoneNumber;
}

