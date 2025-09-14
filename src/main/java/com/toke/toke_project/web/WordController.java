package com.toke.toke_project.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.domain.WordList;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.service.AdminWordService;
import com.toke.toke_project.service.WordListService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/words")
public class WordController {

	private final AdminWordService wordService;
	private final WordListService wordListService; // ✅ 추가
	private final UsersRepository usersRepo;

	@GetMapping
	public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String category,
			@RequestParam(defaultValue = "recent") String mode, @RequestParam(defaultValue = "") String group,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
			Principal principal, Model model) {

		Page<Word> words = wordService.search(q, category, mode, group, page, size);
		model.addAttribute("words", words);
		model.addAttribute("q", q);
		model.addAttribute("category", category);
		model.addAttribute("mode", mode);
		model.addAttribute("group", group);

		// ✅ 로그인한 사용자의 단어장 목록 추가
		if (principal != null) {
			String username = principal.getName(); // 이메일 or username
			Users me = usersRepo.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));

			Set<String> myCustomJp = wordListService.findCustomJapaneseWordsForUser(me.getId());
			model.addAttribute("myCustomJp", myCustomJp);

			Map<Long, String> normJpById = words.getContent().stream().collect(
					Collectors.toMap(Word::getId, w -> wordListService.normalizeJpForCompare(w.getJapaneseWord())));
			model.addAttribute("normJpById", normJpById);

			List<WordList> myLists = wordListService.findMine(me.getId(), null, null);
			model.addAttribute("myLists", myLists);
		}

		return "words/list";
	}

	@GetMapping("/words")
	public String listWords(@RequestParam(required = false) String mode, @RequestParam(required = false) String q,
			@RequestParam(required = false) String category, @RequestParam(required = false) int page,
			@RequestParam(required = false) int size, Model model) {
		// 'mode'에 맞춰 단어 목록을 정렬
		Page<Word> words = wordService.search(q, category, mode, null, page, size);

		// 페이지에 단어 목록과 기타 필요한 데이터를 모델에 추가
		model.addAttribute("words", words.getContent());
		model.addAttribute("q", q);
		model.addAttribute("category", category);
		model.addAttribute("mode", mode);

		return "list"; // list.html로 반환
	}
}
