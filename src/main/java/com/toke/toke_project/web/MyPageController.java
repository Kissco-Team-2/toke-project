package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.security.CustomUserDetails;
import com.toke.toke_project.service.MyPageQueryService;
import com.toke.toke_project.service.MyPageService;
import com.toke.toke_project.web.dto.MyPageForm;
import com.toke.toke_project.web.dto.MyPageView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
@PreAuthorize("isAuthenticated()")
public class MyPageController {

    private final MyPageService myPageService;
    private final MyPageQueryService queryService;
    private final UsersRepository usersRepo;

    @GetMapping
    public String page(@AuthenticationPrincipal CustomUserDetails principal,
                       @RequestParam(defaultValue = "qna") String tab,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        if (principal == null) return "redirect:/login";
        Long userId = principal.getId();

        Users me = usersRepo.findById(userId).orElseThrow();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        // 프로필 폼 세팅
        MyPageForm form = new MyPageForm();
        form.setEmail(me.getEmail());
        form.setNickname(me.getNickname());
        form.setPhoneNumber(me.getPhoneNumber());

        // 뷰 모델
        MyPageView view = MyPageView.builder()
                .form(form)
                .activeTab(tab)
                .build();

        switch (tab) {
            case "qna" -> {
                // 아코디언용 QnA 페이지 (본문/답변 포함)
                model.addAttribute("qnaAccordionPage",
                        queryService.findMyQnaAccordion(userId, pageable));
            }
            case "wordbook" -> {
                view.setWordbooks(queryService.findMyWordbooks(userId, pageable));
            }
            case "profile" -> {
                // 폼만 표시 (view.form)
            }
            default -> {
                // 안전하게 qna로 포워딩
                model.addAttribute("qnaAccordionPage",
                        queryService.findMyQnaAccordion(userId, pageable));
                view.setActiveTab("qna");
            }
        }

        model.addAttribute("view", view);
        model.addAttribute("form", form); // 템플릿이 ${form} 직접 참조 시 필요
        return "mypage/index";
    }

    @PostMapping("/update")
    public String update(@AuthenticationPrincipal CustomUserDetails principal,
                         @Valid @ModelAttribute("form") MyPageForm form,
                         BindingResult binding,
                         RedirectAttributes ra,
                         Model model) {
        if (principal == null) return "redirect:/login";

        if (binding.hasErrors()) {
            MyPageView view = new MyPageView();
            view.setForm(form);
            view.setActiveTab("profile");
            model.addAttribute("view", view);
            return "mypage/index";
        }

        try {
            myPageService.updateMe(principal.getId(),
                    form.getEmail().trim(),
                    form.getNickname().trim(),
                    form.getPhoneNumber().trim());
            ra.addFlashAttribute("msg", "개인정보가 수정되었습니다.");
            return "redirect:/mypage?tab=profile";
        } catch (IllegalStateException e) {
            binding.reject("", e.getMessage());
            MyPageView view = new MyPageView();
            view.setForm(form);
            view.setActiveTab("profile");
            model.addAttribute("view", view);
            return "mypage/index";
        }
    }

    @PostMapping("/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetails principal,
                         RedirectAttributes ra,
                         HttpServletRequest request,
                         HttpServletResponse response) {

        if (principal == null) return "redirect:/login";

        myPageService.deleteMe(principal.getId());

        // ✅ 세션/인증 완전 제거
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        ra.addFlashAttribute("msg", "회원탈퇴가 처리되었습니다.");
        return "redirect:/login?logout=true";
    }
}
