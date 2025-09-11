// src/main/java/com/toke/toke_project/web/QnaController.java
package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.service.QnaService;
import com.toke.toke_project.web.dto.QnaCommentRow;
import com.toke.toke_project.web.dto.QnaDetailView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/qna")
public class QnaController {

    private final QnaService qnaService;
    private final UsersRepository usersRepo;

    /* ===== 목록 (GET /qna) ===== */
    @GetMapping
    public String list(Authentication authentication,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "title") String field,  // UI 파라미터 유지용
                       @RequestParam(name = "q", required = false) String keyword,
                       Model model) {

        Long me = optionalUserId(authentication); // 익명 허용
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");

        Page<?> pageDto = qnaService.pageForUser(me, isAdmin, PageRequest.of(page, size));
        model.addAttribute("page", pageDto);
        model.addAttribute("field", field);
        model.addAttribute("keyword", keyword);
        return "qna/list";
    }

    /* ===== 폼 (GET /qna/form) ===== */
    @GetMapping("/form")
    public String form() {
        return "qna/form";
    }

    /* ===== 작성 (POST /qna) ===== */
    @PostMapping
    public String create(Authentication authentication,
                         @RequestParam String category,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(name = "secret", defaultValue = "off") String secretFlag) {
        Long me = currentUserId(authentication); // 로그인 필수
        boolean secret = "on".equalsIgnoreCase(secretFlag) || "true".equalsIgnoreCase(secretFlag);
        Long id = qnaService.write(me, category, title, content, secret);
        return "redirect:/qna/" + id;
    }

    /* ===== 상세 (GET /qna/{id}) ===== */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         Authentication authentication,
                         Model model) {
        Long me = optionalUserId(authentication); // 익명 허용
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");

        QnaDetailView q = qnaService.detailForUser(id, me, isAdmin);
        boolean isOwner = (me != null) && qnaService.isOwner(id, me);
        List<QnaCommentRow> comments = qnaService.listComments(id);

        model.addAttribute("q", q);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("comments", comments);
        model.addAttribute("titleMine", isOwner);
        return "qna/detail";
    }

    /* ===== 관리자 답변 등록 (POST /qna/{id}/reply) ===== */
    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id,
                        Authentication authentication,
                        @RequestParam String reply) {
        requireRole(authentication, "ROLE_ADMIN");
        Long adminId = currentUserId(authentication);
        qnaService.addReply(id, reply, adminId);
        return "redirect:/qna/" + id;
    }

    /* ===== 삭제 (POST /qna/{id}/delete) ===== */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Authentication authentication) {
        Long me = currentUserId(authentication);
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        qnaService.delete(id, me, isAdmin);
        return "redirect:/qna";
    }

    /* ================= 공통 유틸 ================= */

    /** 로그인 필수: 없으면 403 */
    private Long currentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        String username; // 우리 프로젝트는 email을 username으로 사용
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            username = ud.getUsername();
        } else {
            username = auth.getName(); // fallback
        }
        Users u = usersRepo.findByEmail(username).orElseThrow();
        return u.getId();
    }

    /** 로그인 선택: 없으면 null */
    private Long optionalUserId(Authentication auth) {
        try {
            return currentUserId(auth);
        } catch (AccessDeniedException ignored) {
            return null;
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    private void requireRole(Authentication auth, String role) {
        if (!hasRole(auth, role)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }
}
