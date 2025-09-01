package com.toke.toke_project.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterForm {
	@NotBlank @Size(min = 2, max = 30)
	private String username;
	
	@NotBlank @Email
	private String email;
	
	// 한글/영문/숫자만 1~6자
	@NotBlank
	@Pattern(regexp = "^[A-Za-z0-9가-힣]{1,6}$", message = "닉네임은 한글/영문/숫자 1~6자만 가능합니다.")
	private String nickname;
	
	@NotBlank @Size(min = 8, max = 50)
    private String password;

    @NotBlank
    private String confirmPassword;

}
