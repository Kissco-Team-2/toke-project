package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.domain.WordList;
import com.toke.toke_project.service.WordListService;
import com.toke.toke_project.web.dto.CustomWordForm;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

/**
 * 단어장 관련 요청 처리 컨트롤러
 *
 * 경로: GET /lists -> 모두의 단어장 (index) GET /lists/mine -> 내 단어장 (로그인 필요) GET
 * /lists/new -> 생성 폼 (로그인 필요) POST /lists -> 생성 처리 (로그인 필요) GET /lists/{id} ->
 * 상세 (항목 포함) POST /lists/{id}/edit -> 수정 (소유자만) POST /lists/{id}/delete -> 삭제
 * (소유자만) POST /lists/{id}/items/addWord -> 공식 단어 추가 (소유자만) POST
 * /lists/{id}/items/addCustom -> 커스텀 단어 추가 (소유자만) POST
 * /lists/{id}/items/{itemId}/delete -> 항목 삭제 (소유자만) POST
 * /lists/{id}/items/{itemId}/customize -> 공식->커스텀 전환 POST
 * /lists/{id}/items/{itemId}/editCustom -> 커스텀 항목 수정 GET /lists/search -> 검색
 * (제목/닉네임/태그)
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/lists")
public class WordListController {

	private final WordListService wordListService;
	private final com.toke.toke_project.repo.UsersRepository usersRepo;

	// --- 모두의 단어장 ---
	@GetMapping
	public String all(Model model) {
		List<WordList> lists = wordListService.findAll();
		model.addAttribute("lists", lists);
		model.addAttribute("groups", chunkLists(lists, 3)); // 화면에서 3개씩 묶어 보여줄 때 사용
		return "/lists/index";
	}

	// --- 내 단어장 (로그인 필요) ---
	@GetMapping("/mine")
	public String mine(Principal principal, Model model) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		List<WordList> lists = wordListService.findMine(me);
		model.addAttribute("lists", lists);
		model.addAttribute("groups", chunkLists(lists, 3));
		return "lists/index";
	}

	// --- 생성 폼 (로그인 필요) ---
	@GetMapping("/new")
	public String newForm(Principal principal) {
		if (principal == null)
			return "redirect:/login";
		return "lists/new";
	}

	// --- 생성 처리 (로그인 필요) ---
	@PostMapping
	public String create(Principal principal, @RequestParam String listName,
			@RequestParam(required = false) String description, @RequestParam(required = false) String tags,
			RedirectAttributes redirectAttrs) {
		if (principal == null)
			return "redirect:/login";

		Long me = currentUserId(principal);
		List<String> tagList = splitTags(tags);
		Long id = wordListService.createList(me, listName, description, tagList);

		// 생성 후 인덱스에서 강조하고 싶으면 flash로 id 전달 가능
		redirectAttrs.addFlashAttribute("createdId", id);
		// 인덱스로 이동해서 방금 만든 카드가 보이게 함
		return "redirect:/lists";
	}

	// --- 상세 (공개) ---
	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Principal principal, Model model) {
	    var map = wordListService.getDetail(id);
	    WordList wl = (WordList) map.get("list");

	    // 단어장 정보와 아이템 목록
	    model.addAttribute("list", map.get("list"));
	    model.addAttribute("items", map.get("items"));
	    model.addAttribute("listTags", wl.getTags()); 
	    model.addAttribute("customWordForm", new CustomWordForm());
	    
	    if(principal != null){
	        Long me = currentUserId(principal); // 로그인 사용자 ID 가져오기
	        model.addAttribute("me", me);
	    }


	    return "lists/detail";  // detail.html에서 modal 포함
	}

	// --- 수정(제목/설명/태그) (소유자만) ---
	@PostMapping("/{id}/edit")
	public String edit(@PathVariable Long id, Principal principal, @RequestParam String listName,
			@RequestParam(required = false) String description, @RequestParam(required = false) String tags) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.updateList(id, me, listName, description, splitTags(tags));
		return "redirect:/lists/" + id;
	}

	// --- 삭제 (소유자만) ---
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, Principal principal) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.deleteList(id, me);
		return "redirect:/lists/mine";
	}

	// --- 아이템 추가(공식 단어) ---
	@PostMapping("/{id}/items/addWord")
	public String addWordItem(@PathVariable Long id, Principal principal, @RequestParam Long wordId) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.addItemFromWord(id, me, wordId);
		return "redirect:/lists/" + id;
	}

	// --- 아이템 추가(커스텀) ---
	@PostMapping("/{id}/items/addCustom")
	public String addCustomItem(
			@PathVariable Long id, 
			Principal principal, 
			@Valid @ModelAttribute("customWordForm") CustomWordForm form,
			BindingResult bindingResult,
			Model model
			) {
		
		if (principal == null)
			return "redirect:/login";
		
		Long me = currentUserId(principal);
		
		if(bindingResult.hasErrors()) {
			 var map = wordListService.getDetail(id);
			 WordList wl = (WordList) map.get("list");

		        model.addAttribute("list", map.get("list"));
		        model.addAttribute("items", map.get("items"));
		        model.addAttribute("listTags", wl.getTags());
		        model.addAttribute("customWordForm", form); 
			return "lists/detail";
		}
		wordListService.addCustomItem(id, me, form.getJp(), form.getKana(), form.getKr(), form.getEx());
		return "redirect:/lists/" + id;
	}

	// --- 아이템 삭제 ---
	@PostMapping("/{id}/items/{itemId}/delete")
	public String deleteItem(@PathVariable Long id, @PathVariable Long itemId, Principal principal) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.removeItem(itemId, me);
		return "redirect:/lists/" + id;
	}

	// --- 검색 (제목/닉네임/태그) ---
	@GetMapping("/search")
	public String search(@RequestParam(required = false) String keyword, Principal principal, Model model) {
		// 하나의 입력값으로 제목, 닉네임, 태그 모두 검색
		if(principal == null) return "redirect:/login";
		
		List<WordList> lists = wordListService.search(keyword, keyword, keyword);

		// 검색 결과 모델에 전달
		model.addAttribute("lists", lists);
		model.addAttribute("keyword", keyword); // 검색어 전달
		model.addAttribute("groups", chunkLists(lists, 3)); // 3개씩 묶어서 보여줌
		return "lists/index";
	}

	// --- 공식 -> 커스텀 사본 전환 (소유자만) ---
	@PostMapping("/{id}/items/{itemId}/customize")
	public String customize(@PathVariable Long id, @PathVariable Long itemId, Principal principal) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.customizeFromOfficial(itemId, me);
		return "redirect:/lists/" + id;
	}

	// --- 커스텀 항목 수정 (소유자만) ---
	@PostMapping("/{id}/items/{itemId}/editCustom")
	public String editCustom(@PathVariable Long id, @PathVariable Long itemId, Principal principal,
			@RequestParam String jp, @RequestParam(required = false) String kana,
			@RequestParam(required = false) String kr, @RequestParam(required = false) String ex) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.updateCustomItem(itemId, me, jp, kana, kr, ex);
		return "redirect:/lists/" + id;
	}

	// --- helper: Principal -> userId (401 처리) ---
	private Long currentUserId(Principal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		String username = principal.getName(); // 보통 email 또는 username
		Users u = usersRepo.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		return u.getId();
	}

	// --- helper: 태그 문자열 분리 (폼에서 콤마/공백 등으로 입력된 경우) ---
	private List<String> splitTags(String tags) {
		if (tags == null || tags.isBlank())
			return Collections.emptyList();
		return Arrays.stream(tags.split("[,#\\s]+")).map(String::trim).filter(s -> !s.isBlank()).toList();
	}

	// --- helper: 리스트를 chunkSize 단위로 묶어서 뷰에 전달 (캐러셀 등에서 사용) ---
	private List<List<WordList>> chunkLists(List<WordList> lists, int chunkSize) {
		if (lists == null || lists.isEmpty())
			return Collections.emptyList();
		List<List<WordList>> groups = new ArrayList<>();
		for (int i = 0; i < lists.size(); i += chunkSize) {
			int end = Math.min(i + chunkSize, lists.size());
			groups.add(new ArrayList<>(lists.subList(i, end))); // 복사하여 안전하게 전달
		}
		return groups;
	}

}
