package com.toke.toke_project.web;

import java.util.Optional;
import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ForgotEmailController {
	private final UsersRepository usersRepository;

	@PostMapping("/forgot/email")
	public String findEmail(@RequestParam String username, @RequestParam String phoneNumber,
			org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
		Optional<Users> userOpt = usersRepository.findByUsernameAndPhoneNumber(username, phoneNumber);
		if (userOpt.isPresent()) {
			ra.addFlashAttribute("email", userOpt.get().getEmail());
		} else {
			ra.addFlashAttribute("error", "일치하는 회원 정보를 찾을 수 없습니다.");
		}
		// 해시를 붙여 이메일 탭이 열리게 힌트 제공(선택)
		return "redirect:/login#find-email";
	}

}

//@PostMapping("/forgot/email")
//public String findEmail(@RequestParam String username, 
//		@RequestParam String phoneNumber,
//		Model model) {
//	Optional<Users> userOpt = usersRepository.findByUsernameAndPhoneNumber(username, phoneNumber);
//	if (userOpt.isPresent()) {
//		model.addAttribute("email", userOpt.get().getEmail());
//	} else {
//		model.addAttribute("error", "일치하는 회원 정보를 찾을 수 없습니다.");
//	}
//	return "auth/result_email";
//}