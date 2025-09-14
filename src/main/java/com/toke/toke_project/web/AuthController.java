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

		if (binding.hasErrors()) {
			return "auth/register";
		}

		try {
			authService.register(form);
			ra.addFlashAttribute("msg", "회원가입이 성공적으로 완료되었습니다.");
			return "redirect:/register_success"; // ✅ 리다이렉트로 이동
		} catch (IllegalArgumentException | IllegalStateException e) {
			binding.reject("", e.getMessage());
			return "auth/register";
		}
	}

	@GetMapping("/login")
	public String loginPage(@RequestParam(value = "error", required = false) String error,
			RedirectAttributes ra, Model model) {
		// 로그인 실패 파라미터가 있으면 flash에 메시지를 담고 쿼리 제거를 위해 리다이렉트
		if (error != null) {
			ra.addFlashAttribute("loginError", "이메일 혹은 비밀번호가 틀렸거나 존재하지 않는 계정입니다.");
			return "redirect:/login";
		}

		// 플래시 속성(loginError)은 리다이렉트 후 모델에 자동으로 들어옵니다.
		return "auth/login"; // 템플릿 이름
	}

	@GetMapping("/register_success")
	public String registerSuccessPage() {
		return "auth/register_success";
	}
}
