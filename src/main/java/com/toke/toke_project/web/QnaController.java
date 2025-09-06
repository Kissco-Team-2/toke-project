package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.service.QnaService;
import com.toke.toke_project.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/qna")
public class QnaController {

    private final QnaService qnaService;
    private final UsersRepository usersRepo;

    @GetMapping
    public String list(@AuthenticationPrincipal User principal, Model model) {
        Long me = principal == null ? -1L : currentUserId(principal);
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("list", qnaService.listForUser(me, isAdmin));
        model.addAttribute("isAdmin", isAdmin);
        return "qna/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal User principal,
                         Model model) {
        Long me = principal == null ? -1L : currentUserId(principal);
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        var q = qnaService.detailForUser(id, me, isAdmin);
        model.addAttribute("q", q);
        model.addAttribute("isAdmin", isAdmin);
        return "qna/detail";
    }

    @GetMapping("/new")
    public String form() { return "qna/new"; }

    @PostMapping
    public String create(@AuthenticationPrincipal User principal,
                         @RequestParam String category,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(defaultValue = "false") boolean secret) {
        Long me = currentUserId(principal);
        Long id = qnaService.write(me, category, title, content, secret);
        return "redirect:/qna/" + id;
    }

    // ✅ 관리자 답변
    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        @AuthenticationPrincipal User principal,
                        @RequestParam String reply) {
        Long adminId = currentUserId(principal);
        qnaService.addReply(id, reply, adminId);
        return "redirect:/qna/" + id;
    }

    // 상태 종료(본인 또는 관리자)
    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id,
                        @AuthenticationPrincipal User principal) {
        Long me = currentUserId(principal);
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        qnaService.close(id, me, isAdmin);
        return "redirect:/qna/" + id;
    }

    private Long currentUserId(User principal) {
        Users u = usersRepo.findByEmail(principal.getUsername()).orElseThrow();
        return u.getId();
    }
}
