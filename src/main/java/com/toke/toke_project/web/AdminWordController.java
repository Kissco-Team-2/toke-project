package com.toke.toke_project.web;
//컨트롤러 (관리자 라우트) URL과 화면 연결. 폼 검증 실패 시 그대로 폼 재표시
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.service.AdminWordService;
import com.toke.toke_project.web.dto.WordForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/words")
public class AdminWordController {

    private final AdminWordService wordService;

    // 전체 목록
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String category,
                       Model model) {
        List<Word> words = wordService.list(q, category);
        model.addAttribute("words", words);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        return "admin/words/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new WordForm());
        return "admin/words/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") WordForm form,
                         BindingResult binding,
                         RedirectAttributes ra,
                         @AuthenticationPrincipal User principal) {
        if (binding.hasErrors()) return "admin/words/form";

        Long adminUserId = 1L; // TODO: principal로부터 users.user_id 찾기
        Long id = wordService.create(form, adminUserId);
        ra.addFlashAttribute("msg", "단어가 등록되었습니다.");
        return "redirect:/admin/words/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Word w = wordService.get(id);
        WordForm f = new WordForm();
        f.setId(w.getId());
        f.setJapaneseWord(w.getJapaneseWord());
        f.setReadingKana(w.getReadingKana());
        f.setKoreanMeaning(w.getKoreanMeaning());
        f.setCategory(w.getCategory());
        f.setExampleSentenceJp(w.getExampleSentenceJp());
        model.addAttribute("form", f);
        return "admin/words/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") WordForm form,
                         BindingResult binding,
                         RedirectAttributes ra) {
        if (binding.hasErrors()) return "admin/words/form";
        wordService.update(id, form);
        ra.addFlashAttribute("msg", "단어가 수정되었습니다.");
        return "redirect:/admin/words";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        wordService.delete(id);
        ra.addFlashAttribute("msg", "단어가 삭제되었습니다.");
        return "redirect:/admin/words";
    }
}
