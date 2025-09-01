package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.service.MailService;
import com.toke.toke_project.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;

@Controller
@RequestMapping("/forgot/password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UsersRepository usersRepo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final VerificationService verificationService;

    /** 1) 이메일 입력 페이지 */
    @GetMapping
    public String page() {
        return "auth/forgot_password";
    }

    /** 1) 인증 코드 전송 */
    @PostMapping("/send")
    public String send(@RequestParam String email, RedirectAttributes ra) {
        // 가입 여부 확인
        Users user = usersRepo.findByEmail(email).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("err", "가입된 이메일이 없습니다.");
            return "redirect:/forgot/password";
        }

        // 코드 생성/저장 후 메일 발송 (10분 유효)
        verificationService.createAndSend(
                email,
                "RESET_PASSWORD",
                Duration.ofMinutes(10),
                code -> mailService.sendCode(email, code)
        );

        ra.addFlashAttribute("msg", "인증 코드를 이메일로 보냈습니다.");
        ra.addFlashAttribute("email", email); // 다음 페이지에서 사용
        return "redirect:/forgot/password/verify";
    }

    /** 2) 코드 입력 페이지 */
    @GetMapping("/verify")
    public String verifyPage(@ModelAttribute("email") String email, Model model) {
        // redirect로 전달된 email이 없을 수 있으니, 비어있으면 처음 페이지로
        if (email == null || email.isBlank()) return "redirect:/forgot/password";
        model.addAttribute("email", email);
        return "auth/forgot_verify";
    }

    /** 2) 코드 검증 처리 */
    @PostMapping("/verify")
    public String verify(@RequestParam String email,
                         @RequestParam String code,
                         RedirectAttributes ra) {
        try {
            verificationService.verify(email, "RESET_PASSWORD", code);
            ra.addFlashAttribute("email", email);
            return "redirect:/forgot/password/reset";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("err", e.getMessage());
            ra.addFlashAttribute("email", email);
            return "redirect:/forgot/password/verify";
        }
    }

    /** 3) 새 비밀번호 입력 페이지 */
    @GetMapping("/reset")
    public String resetPage(@ModelAttribute("email") String email, Model model, RedirectAttributes ra) {
        if (email == null || email.isBlank()) {
            ra.addFlashAttribute("err", "인증을 먼저 완료해주세요.");
            return "redirect:/forgot/password";
        }
        model.addAttribute("email", email);
        return "auth/forgot_reset";
    }

    /** 3) 새 비밀번호 저장 */
    @PostMapping("/reset")
    public String reset(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam String confirm,
                        RedirectAttributes ra) {

        if (!password.equals(confirm)) {
            ra.addFlashAttribute("err", "비밀번호가 일치하지 않습니다.");
            ra.addFlashAttribute("email", email);
            return "redirect:/forgot/password/reset";
        }

        Users user = usersRepo.findByEmail(email).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("err", "가입 정보를 찾을 수 없습니다.");
            return "redirect:/forgot/password";
        }

        user.setPassword(passwordEncoder.encode(password));
        usersRepo.save(user);

        ra.addFlashAttribute("msg", "비밀번호가 변경되었습니다. 로그인 해주세요.");
        return "redirect:/login";
    }
}

