package com.toke.toke_project.web;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.toke.toke_project.domain.Word;
import com.toke.toke_project.service.AdminWordService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/words")
public class WordController {

    private final AdminWordService wordService; // 같은 서비스 재사용

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "recent") String mode, // ko / ja / recent / cat
            @RequestParam(defaultValue = "") String group,      // ✅ 인덱스 필터 (가나다/히라가나)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        // group까지 포함해서 검색
        Page<Word> words = wordService.search(q, category, mode, group, page, size);

        model.addAttribute("words", words);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("mode", mode);
        model.addAttribute("group", group); // ✅ 뷰에서도 사용 가능

        return "words/list"; // 사용자+관리자 공용 화면
    }
}
