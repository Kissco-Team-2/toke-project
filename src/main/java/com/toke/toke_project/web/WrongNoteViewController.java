package com.toke.toke_project.web;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//페이지 렌더링용 (HTML)
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WrongNoteViewController {

    @GetMapping("/wrong-notes")
    public String wrongNotesPage(@AuthenticationPrincipal Object principal) {
        if (principal == null) return "redirect:/auth/login"; // 로그인 페이지 경로에 맞춰 수정
        return "wrong-notes/wrongnote"; // templates/words/wrong-notes/wrongnote.html
    }
}
