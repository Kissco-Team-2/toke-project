package com.toke.toke_project.web;
/*
URL 요청 받아서 서비스 호출 & 뷰 연결.
/lists → 모두의 단어장 보기.
/lists/mine → 내 단어장.
/lists/new → 생성 폼.
/lists/{id} → 상세 페이지(항목 목록 포함).
/lists/{id}/edit → 수정.
/lists/{id}/delete → 삭제.
/lists/{id}/items/... → 항목 추가/삭제.
/lists/search → 제목/닉네임/태그 검색.
*/
import com.toke.toke_project.domain.Users;
import com.toke.toke_project.domain.WordList;
import com.toke.toke_project.service.WordListService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/lists")
public class WordListController {

    private final WordListService wordListService;
    private final com.toke.toke_project.repo.UsersRepository usersRepo;

    /* 모두의 단어장 */
    @GetMapping
    public String all(Model model) {
        model.addAttribute("lists", wordListService.findAll());
        return "lists/index";
    }

    /* 내 단어장 */
    @GetMapping("/mine")
    public String mine(@AuthenticationPrincipal User principal, Model model) {
        Long me = currentUserId(principal);
        model.addAttribute("lists", wordListService.findMine(me));
        return "lists/index";
    }

    /* 생성 폼 */
    @GetMapping("/new")
    public String newForm() { return "lists/new"; }

    /* 생성 처리 */
    @PostMapping
    public String create(@AuthenticationPrincipal User principal,
                         @RequestParam String listName,
                         @RequestParam(required=false) String description,
                         @RequestParam(required=false) String tags) {
        Long me = currentUserId(principal);
        List<String> tagList = splitTags(tags);
        Long id = wordListService.createList(me, listName, description, tagList);
        return "redirect:/lists/" + id;
    }

    /* 상세 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var map = wordListService.getDetail(id);
        model.addAttribute("list", map.get("list"));
        model.addAttribute("items", map.get("items"));
        return "lists/detail";
    }

    /* 수정(제목/설명/태그) */
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @AuthenticationPrincipal User principal,
                       @RequestParam String listName,
                       @RequestParam(required=false) String description,
                       @RequestParam(required=false) String tags) {
        Long me = currentUserId(principal);
        wordListService.updateList(id, me, listName, description, splitTags(tags));
        return "redirect:/lists/" + id;
    }

    /* 삭제 */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal User principal) {
        Long me = currentUserId(principal);
        wordListService.deleteList(id, me);
        return "redirect:/lists/mine";
    }

    /* 아이템 추가(공식 단어) */
    @PostMapping("/{id}/items/addWord")
    public String addWordItem(@PathVariable Long id,
                              @AuthenticationPrincipal User principal,
                              @RequestParam Long wordId) {
        Long me = currentUserId(principal);
        wordListService.addItemFromWord(id, me, wordId);
        return "redirect:/lists/" + id;
    }

    /* 아이템 추가(커스텀) */
    @PostMapping("/{id}/items/addCustom")
    public String addCustomItem(@PathVariable Long id,
                                @AuthenticationPrincipal User principal,
                                @RequestParam String jp,
                                @RequestParam(required=false) String kana,
                                @RequestParam(required=false) String kr,
                                @RequestParam(required=false) String ex) {
        Long me = currentUserId(principal);
        wordListService.addCustomItem(id, me, jp, kana, kr, ex);
        return "redirect:/lists/" + id;
    }

    /* 아이템 삭제 */
    @PostMapping("/{id}/items/{itemId}/delete")
    public String deleteItem(@PathVariable Long id,
                             @PathVariable Long itemId,
                             @AuthenticationPrincipal User principal) {
        Long me = currentUserId(principal);
        wordListService.removeItem(itemId, me);
        return "redirect:/lists/" + id;
    }

    /* 검색 (제목/닉네임/태그) */
    @GetMapping("/search")
    public String search(@RequestParam(required=false) String title,
                         @RequestParam(required=false) String nickname,
                         @RequestParam(required=false) String tag,
                         Model model) {
        List<WordList> lists = wordListService.search(title, nickname, tag);
        model.addAttribute("lists", lists);
        model.addAttribute("titleQ", title);
        model.addAttribute("nickQ", nickname);
        model.addAttribute("tagQ", tag);
        return "lists/index";
    }

    private Long currentUserId(User principal) {
        // username = email 로 가정
        Users u = usersRepo.findByEmail(principal.getUsername()).orElseThrow();
        return u.getId();
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return Collections.emptyList();
        // 콤마/스페이스/해시 섞여도 분리되게
        return Arrays.stream(tags.split("[,#\\s]+"))
                .filter(s -> !s.isBlank())
                .toList();
    }
    
 // 2-1) 공식 → 커스텀 사본 전환
    @PostMapping("/{id}/items/{itemId}/customize")
    public String customize(@PathVariable Long id,
                            @PathVariable Long itemId,
                            @AuthenticationPrincipal User principal) {
        Long me = currentUserId(principal);
        wordListService.customizeFromOfficial(itemId, me);
        return "redirect:/lists/" + id;
    }

    // 2-2) 커스텀 사본 수정
    @PostMapping("/{id}/items/{itemId}/editCustom")
    public String editCustom(@PathVariable Long id,
                             @PathVariable Long itemId,
                             @AuthenticationPrincipal User principal,
                             @RequestParam String jp,
                             @RequestParam(required=false) String kana,
                             @RequestParam(required=false) String kr,
                             @RequestParam(required=false) String ex) {
        Long me = currentUserId(principal);
        wordListService.updateCustomItem(itemId, me, jp, kana, kr, ex);
        return "redirect:/lists/" + id;
    }
}
