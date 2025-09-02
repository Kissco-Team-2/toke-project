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
	@Pattern(
			regexp = "^[A-Za-z0-9가-힣]{1,6}$", 
			message = "닉네임은 한글/영문/숫자 1~6자만 가능합니다.")
	private String nickname;
	
	@NotBlank @Size(min = 8, max = 50)
	@Pattern(
		    regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{10,50}$", 
		    message = "비밀번호는 영문 + 숫자 조합 10자 이상이어야 합니다.")
    private String password;

    @NotBlank
    private String confirmPassword;

}
