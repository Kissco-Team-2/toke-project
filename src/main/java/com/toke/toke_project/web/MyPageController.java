package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.service.MyPageService;
import com.toke.toke_project.web.dto.MyPageForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {
    private final MyPageService myPageService;
    private final UsersRepository usersRepo;

    @GetMapping
    public String page(@AuthenticationPrincipal User principal, Model model) {
        Users me = currentUser(principal);
        MyPageForm f = new MyPageForm();
        f.setEmail(me.getEmail());
        f.setNickname(me.getNickname());
        f.setPhoneNumber(me.getPhoneNumber());
        model.addAttribute("form", f);
        return "mypage/index";
    }

    @PostMapping("/update")
    public String update(@AuthenticationPrincipal User principal,
                         @Valid @ModelAttribute("form") MyPageForm form,
                         BindingResult binding,
                         RedirectAttributes ra,
                         Model model) {
        if (binding.hasErrors()) return "mypage/index";
        Users me = currentUser(principal);
        try {
            myPageService.updateMe(me.getId(), form.getEmail().trim(), form.getNickname().trim(), form.getPhoneNumber().trim());
            ra.addFlashAttribute("msg", "개인정보가 수정되었습니다.");
            return "redirect:/mypage";
        } catch (IllegalStateException e) {
            binding.reject("", e.getMessage());
            return "mypage/index";
        }
    }

    @PostMapping("/delete")
    public String delete(@AuthenticationPrincipal User principal) {
        Users me = currentUser(principal);
        myPageService.deleteMe(me.getId());
        return "redirect:/login?logout=true";
    }

    private Users currentUser(User principal) {
        return usersRepo.findByEmail(principal.getUsername()).orElseThrow();
    }
}
