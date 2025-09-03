package com.toke.toke_project.web;

import com.toke.toke_project.service.AuthService;
import com.toke.toke_project.web.dto.RegisterForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/register")
	public String registerPage(Model model) {
		model.addAttribute("form", new RegisterForm());
		return "auth/register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult binding,
			RedirectAttributes ra) {
		 System.out.println("📌 [DEBUG] register() 호출됨");
		 
		if (binding.hasErrors()) {
			return "auth/register"; // DTO 유효성 에러. 다시 회원가입 폼으로
		}
		try {
			System.out.println("➡️ [DEBUG] 서비스 호출 직전");
			authService.register(form); // 실제 회원가입 처리
			System.out.println("✅ [DEBUG] 서비스 호출 완료 - DB 저장 성공");
			   
			ra.addFlashAttribute("msg", "회원가입이 성공적으로 완료되었습니다.");
			return "auth/register_success"; // 성공시 완료 페이지로 리다이렉트
			
		} catch (IllegalArgumentException | IllegalStateException e) {
			 System.out.println("❌ [DEBUG] 서비스에서 예외 발생: " + e.getMessage());
			binding.reject("", e.getMessage()); // 서비스에서 던진 메시지 표출
			return "auth/register";
		}
	}

	@GetMapping("/login")
	public String loginPage() {
		return "auth/login";
	}

	@GetMapping("/register_success")
	public String registerSuccessPage() {
		return "auth/register_success";
	}
}
