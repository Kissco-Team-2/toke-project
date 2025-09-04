package com.toke.toke_project.web;

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

import org.springframework.data.domain.Page;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/words")
public class AdminWordController {

    private final AdminWordService wordService;

    // 전체 목록 (검색 + 카테고리 + 정렬 + 페이징)
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "recent") String mode,
                       @RequestParam(defaultValue = "") String group,   // ✅ 추가
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {

        Page<Word> words = wordService.search(q, category, mode, group, page, size);

        model.addAttribute("words", words);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("mode", mode);
        model.addAttribute("group", group);

        return "words/list";
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

        Long adminUserId = 1L; // TODO: principal -> users.user_id 매핑
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
