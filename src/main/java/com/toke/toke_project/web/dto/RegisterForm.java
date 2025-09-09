package com.toke.toke_project.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterForm {

    @NotBlank
    @Size(min = 2, max = 30)
    private String username;

    @NotBlank
    @Email
    private String email;

    // 한글/영문/숫자만 1~6자
    @NotBlank
    @Pattern(
        regexp = "^[A-Za-z0-9가-힣]{1,6}$",
        message = "닉네임은 한글/영문/숫자 1~6자만 가능합니다."
    )
    private String nickname;

    // 비밀번호: 영문 + 숫자, 10~50자
    @NotBlank
    @Size(min = 10, max = 50)
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{10,50}$",
        message = "비밀번호는 영문과 숫자 조합으로 10자 이상이어야 합니다."
    )
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    @Pattern(
        regexp = "^[0-9]{11}$",
        message = "전화번호는 숫자 11자리만 입력 가능합니다."
    )
    private String phoneNumber;

    /** 비밀번호 확인 일치 검증 */
    @AssertTrue(message = "비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) return false;
        return password.equals(confirmPassword);
    }
}
