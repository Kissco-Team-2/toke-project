package com.toke.toke_project.web;

import com.toke.toke_project.domain.Hashtag;
import com.toke.toke_project.domain.Users;
import com.toke.toke_project.domain.WordList;
import com.toke.toke_project.domain.WordListItem;
import com.toke.toke_project.repo.HashtagRepository;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.service.WordListService;
import com.toke.toke_project.web.dto.CustomWordForm;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/lists")
public class WordListController {

	private final WordListService wordListService;
	private final UsersRepository usersRepo;
	private final HashtagRepository hashtagRepository; // ✅ 추가

	// --- 모두의 단어장 ---

	@GetMapping
	public String all(@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "tag", required = false) List<String> tags, Model model) {

		List<WordList> lists = wordListService.search(keyword, null, tags);

		if (lists == null) {
			lists = Collections.emptyList();
		} else {
			lists = lists.stream().filter(Objects::nonNull).toList();
		}

		List<WordList> sharedLists = lists.stream().filter(wl -> Objects.equals(wl.getIsShared(), 1))
				.collect(Collectors.toList());

		model.addAttribute("sharedLists", sharedLists);
		model.addAttribute("lists", lists);

		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedTags", tags == null ? Collections.emptyList() : tags);
		model.addAttribute("groups", chunkLists(sharedLists, 3));
		model.addAttribute("allTags", hashtagRepository.findAll());

		model.addAttribute("title", "모두의 단어장");
		model.addAttribute("isMineView", false);

		return "/lists/index";
	}

	@GetMapping("/mine")
	public String mine(@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "tag", required = false) List<String> tags, Principal principal, Model model) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);

		List<WordList> lists = wordListService.findMine(me, keyword, tags);

		model.addAttribute("lists", lists);
		model.addAttribute("keyword", keyword);
		model.addAttribute("selectedTags", tags == null ? Collections.emptyList() : tags);
		// 기존에 단일 tag를 쓰는 뷰 코드와 호환시키려면 첫 요소를 'tag'로도 넣어두자
		model.addAttribute("tag", (tags != null && !tags.isEmpty()) ? tags.get(0) : null);

		model.addAttribute("groups", chunkLists(lists, 3));
		model.addAttribute("allTags", hashtagRepository.findAll());
		model.addAttribute("title", "내 단어장");
		model.addAttribute("isMineView", true);

		return "lists/mine";
	}

	// --- 상세 (공개) ---
	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Principal principal, Model model) {
		var map = wordListService.getDetail(id);
		WordList wl = (WordList) map.get("list");

		List<WordListItem> items = wordListService.findItemsByListIdDesc(id);

		model.addAttribute("list", wl);
		model.addAttribute("items", items);
		model.addAttribute("listTags", wl.getTags());
		model.addAttribute("customWordForm", new CustomWordForm());
		model.addAttribute("ownerNickname", map.get("ownerNickname"));

		Long me = null;

		if (principal != null) {
			me = currentUserId(principal);
			model.addAttribute("me", me);
		}

		boolean canManage = false;
		if (me != null) {
			canManage = Objects.equals(me, wl.getOwner().getId()) || wordListService.isAdmin(me);
		}
		model.addAttribute("canManage", canManage);

		return "lists/detail";
	}

	@GetMapping("/new")
	public String newListForm(Principal principal, Model model) {
		if (principal == null)
			return "redirect:/login";
		model.addAttribute("lists", new WordList());
		return "lists/new";
	}

	@PostMapping("/new")
	public String createList(@RequestParam String listName, @RequestParam(required = false) String description,
			@RequestParam(required = false) String tags, Principal principal) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);

		wordListService.createList(me, listName, description, splitTags(tags));

		return "redirect:/lists/mine";
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

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Principal principal, Model model) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);

		var map = wordListService.getDetail(id);
		WordList wl = (WordList) map.get("list");

		if (!wl.getOwner().getId().equals(me)) {
			throw new SecurityException("권한 없음");
		}

		model.addAttribute("list", wl);
		model.addAttribute("listTags", wl.getTags().stream().map(Hashtag::getTagName).toList());
		return "lists/edit"; // templates/lists/edit.html
	}

	@PostMapping("/{listId}/share")
	public String shareList(@PathVariable Long listId, Principal principal) {
		System.out.println("listId: " + listId);

		wordListService.shareList(listId);
		return "redirect:/lists";
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

	// --- 여러 공식 단어를 내 단어장에 추가 (소유자만) ---
	@PostMapping("/addWords")
	public String addWordsToMyList(@RequestParam("selectedWordIds") String selectedWordIds,
			@RequestParam("listId") Long listId, Principal principal, RedirectAttributes ra) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);

		if (selectedWordIds == null || selectedWordIds.isBlank()) {
			ra.addFlashAttribute("msg", "추가할 단어를 선택하세요.");
			return "redirect:/words";
		}

		// "1,2,3" → [1L, 2L, 3L]
		List<Long> wordIds = Arrays.stream(selectedWordIds.split(",")).filter(s -> !s.isBlank()).map(Long::parseLong)
				.toList();

		wordListService.addWordsToMyList(listId, me, wordIds);

		ra.addFlashAttribute("msg", "선택한 단어가 내 단어장에 추가되었습니다.");
		return "redirect:/lists/" + listId;
	}

	// --- 아이템 추가(커스텀) ---
	@PostMapping("/{id}/items/addCustom")
	public String addCustomItem(@PathVariable Long id, Principal principal,
			@Valid @ModelAttribute("customWordForm") CustomWordForm form, BindingResult bindingResult, Model model) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);

		// 먼저 기본 검증 에러가 있는지 확인
		if (bindingResult.hasErrors()) {
			var map = wordListService.getDetail(id);
			WordList wl = (WordList) map.get("list");

			model.addAttribute("list", map.get("list"));
			model.addAttribute("items", map.get("items"));
			model.addAttribute("listTags", wl.getTags());
			model.addAttribute("customWordForm", form);

			// 모달 자동 오픈 플래그
			model.addAttribute("openNewModal", true);
			return "lists/detail";
		}

		// 중복 체크: 같은 일본어 표현이 이미 단어장에 있는지
		if (wordListService.existsJapaneseInList(id, form.getJp())) {
			bindingResult.rejectValue("jp", "duplicate", form.getJp() + " 는 이미 단어장에 존재합니다.");

			var map = wordListService.getDetail(id);
			WordList wl = (WordList) map.get("list");

			model.addAttribute("list", map.get("list"));
			model.addAttribute("items", map.get("items"));
			model.addAttribute("listTags", wl.getTags());
			model.addAttribute("customWordForm", form);
			model.addAttribute("openNewModal", true);
			return "lists/detail";
		}

		// 통과하면 저장
		wordListService.addCustomItem(id, me, form.getJp(), form.getKana(), form.getKr(), form.getEx());
		return "redirect:/lists/" + id;
	}

	// --- 아이템 삭제 (소유자만) ---
	@PostMapping("/{id}/items/{itemId}/delete")
	public String deleteItem(@PathVariable Long id, @PathVariable Long itemId, Principal principal) {
		if (principal == null)
			return "redirect:/login";
		Long me = currentUserId(principal);
		wordListService.removeItem(itemId, me);
		return "redirect:/lists/" + id;
	}

	@GetMapping("/search")
	public String search(@RequestParam(required = false) String keyword,
			@RequestParam(value = "tag", required = false) List<String> tags, Principal principal, Model model) {
		if (principal == null)
			return "redirect:/login";

		List<WordList> lists = wordListService.search(keyword, keyword, tags);

		model.addAttribute("lists", lists);
		model.addAttribute("keyword", keyword);
		model.addAttribute("groups", chunkLists(lists, 3));
		model.addAttribute("selectedTags", tags == null ? Collections.emptyList() : tags);
		model.addAttribute("allTags", hashtagRepository.findAll());

		model.addAttribute("title", "검색 결과");
		return "lists/index";
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

	// --- helper: 로그인 사용자 ID 조회 ---
	private Long currentUserId(Principal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		String username = principal.getName(); // 보통 email 또는 username
		Users u = usersRepo.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		return u.getId();
	}

	// --- helper: 태그 분리 ---
	private List<String> splitTags(String tags) {
		if (tags == null || tags.isBlank())
			return Collections.emptyList();
		return Arrays.stream(tags.split("[,#\\s]+")).map(String::trim).filter(s -> !s.isBlank()).toList();
	}

	// --- helper: 리스트를 chunkSize 단위로 묶어서 ---
	private List<List<WordList>> chunkLists(List<WordList> lists, int chunkSize) {
		if (lists == null || lists.isEmpty())
			return Collections.emptyList();
		List<List<WordList>> groups = new ArrayList<>();
		for (int i = 0; i < lists.size(); i += chunkSize) {
			int end = Math.min(i + chunkSize, lists.size());
			groups.add(new ArrayList<>(lists.subList(i, end)));
		}
		return groups;
	}
}