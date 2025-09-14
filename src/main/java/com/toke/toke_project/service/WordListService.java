package com.toke.toke_project.service;

/*
단어장 생성/수정/삭제 (소유자만 가능).
단어 항목 추가 (공식 단어 or 커스텀 입력).
단어 항목 삭제.
태그 attach & normalized 처리(중복/경합 대응).
모두의 단어장/내 단어장/검색 기능.
단어장 상세(항목까지) 조회.
 */
import com.toke.toke_project.domain.*;
import com.toke.toke_project.repo.*;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WordListService {

	private final WordListRepository wordListRepo;
	private final WordListItemRepository itemRepo;
	private final WordRepository wordRepo;
	private final HashtagRepository hashtagRepo;
	private final WordListTagRepository wltRepo;
	private final UsersRepository usersRepo;
	private static final Logger logger = LoggerFactory.getLogger(WordListService.class);

	public boolean isAdmin(Long userId) {
		if (userId == null)
			return false;
		return usersRepo.findById(userId).map(u -> {
			String role = u.getRole();
			if (role == null)
				return false;
			role = role.trim();
			return "ROLE_ADMIN".equals(role) || "ADMIN".equalsIgnoreCase(role);
		}).orElse(false);

	}

	private boolean isOwnerOrAdmin(WordList wl, Long userId) {
		if (wl == null || userId == null)
			return false;
		// 소유자 검사
		if (wl.getOwner() != null && Objects.equals(wl.getOwner().getId(), userId)) {
			return true;
		}

		Optional<com.toke.toke_project.domain.Users> ou = usersRepo.findById(userId);
		if (ou.isEmpty())
			return false;
		String role = ou.get().getRole();
		if (role == null)
			return false;
		role = role.toUpperCase();
		return role.equals("ROLE_ADMIN") || role.equals("ADMIN") || role.contains("ADMIN");
	}

	/* 단어장 생성 */
	@Transactional
	public Long createList(Long ownerId, String name, String desc, List<String> tags) {
		Users owner = usersRepo.findById(ownerId).orElseThrow();
		WordList wl = new WordList();
		wl.setOwner(owner);
		wl.setListName(name);
		wl.setDescription(desc);
		wordListRepo.save(wl);

		if (tags != null)
			attachTags(wl.getId(), tags);
		return wl.getId();
	}

	/* 단어장 수정(본인만) */

	@Transactional
	public void updateList(Long listId, Long ownerId, String name, String desc, List<String> tags) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!isOwnerOrAdmin(wl, ownerId)) {
			throw new SecurityException("권한 없음");
		}
		if (name != null)
			wl.setListName(name);
		if (desc != null)
			wl.setDescription(desc);

		if (tags != null) {
			wltRepo.deleteByListId(listId); // 기존 태그 전부 제거 후
			attachTags(listId, tags); // 새 태그 부착
		}
	}

	/* 단어장 삭제(본인만) */

	@Transactional
	public void deleteList(Long listId, Long ownerId) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!isOwnerOrAdmin(wl, ownerId))
			throw new SecurityException("권한 없음");

		wl.getTags().clear();
		itemRepo.deleteByWordList_Id(listId);
		wordListRepo.delete(wl);
	}

	@Transactional
	public Long addItemFromWord(Long listId, Long ownerId, Long wordId) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!isOwnerOrAdmin(wl, ownerId))
			throw new SecurityException("권한 없음");
		Word w = wordRepo.findById(wordId).orElseThrow();

		WordListItem it = new WordListItem();
		it.setWordList(wl);
		it.setWord(w);
		itemRepo.save(it);
		return it.getId();
	}

	@Transactional
	public void addWordsToMyList(Long listId, Long ownerId, List<Long> wordIds) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!isOwnerOrAdmin(wl, ownerId)) {
			throw new SecurityException("권한 없음");
		}

		for (Long wordId : wordIds) {
			addItemAsCustomCopy(listId, ownerId, wordId);
		}
	}

	@Transactional
	public Long addCustomItem(Long listId, Long ownerId, String jp, String kana, String kr, String ex) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!isOwnerOrAdmin(wl, ownerId))
			throw new SecurityException("권한 없음");

		WordListItem it = new WordListItem();
		it.setWordList(wl);
		it.setCustomJapaneseWord(jp);
		it.setCustomReadingKana(kana);
		it.setCustomKoreanMeaning(kr);
		it.setCustomExampleSentenceJp(ex);
		itemRepo.save(it);
		return it.getId();
	}

	// 단어장에 동일한 일본어(공식 단어 또는 customJapaneseWord)가 존재하는지 검사
	@Transactional(readOnly = true)
	public boolean existsJapaneseInList(Long listId, String jp) {
	    if (jp == null) return false;
	    String norm = jp.trim().toLowerCase();

	    // itemRepo 에 적절한 조회 메서드가 없다면 여기서 전체 항목을 불러와 검사
	    List<WordListItem> items = itemRepo.findByWordList_Id(listId);
	    for (WordListItem it : items) {
	        // custom 필드 비교
	        if (it.getCustomJapaneseWord() != null
	                && it.getCustomJapaneseWord().trim().toLowerCase().equals(norm)) {
	            return true;
	        }
	        // 공식 단어 연결이 있는 경우 공식 단어의 japaneseWord 비교
	        if (it.getWord() != null
	                && it.getWord().getJapaneseWord() != null
	                && it.getWord().getJapaneseWord().trim().toLowerCase().equals(norm)) {
	            return true;
	        }
	    }
	    return false;
	}

	@Transactional
	public Long addItemAsCustomCopy(Long listId, Long ownerId, Long wordId) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!isOwnerOrAdmin(wl, ownerId)) {
			throw new SecurityException("권한 없음");
		}
		Word w = wordRepo.findById(wordId).orElseThrow();

		WordListItem it = new WordListItem();
		it.setWordList(wl);

		// 공식 단어 내용을 커스텀 필드로 복사
		it.setCustomJapaneseWord(w.getJapaneseWord());
		it.setCustomReadingKana(w.getReadingKana());
		it.setCustomKoreanMeaning(w.getKoreanMeaning());
		it.setCustomExampleSentenceJp(w.getExampleSentenceJp());

		// Word 참조는 null 처리 (공식 연결 안 함)
		it.setWord(null);

		itemRepo.save(it);
		return it.getId();
	}

	@Transactional
	public void removeItem(Long listItemId, Long ownerId) {
		WordListItem it = itemRepo.findById(listItemId).orElseThrow();
		if (!isOwnerOrAdmin(it.getWordList(), ownerId))
			throw new SecurityException("권한 없음");
		itemRepo.delete(it);
	}

	/* 태그 부착: normalized 중복 방지 + UNIQUE 경합 대응 + 연결 중복 방지 */
	@Transactional
	public void attachTags(Long listId, List<String> tags) {
		if (tags == null || tags.isEmpty())
			return;

		for (String raw : tags) {
			String norm = normalizeTag(raw);
			if (norm.isEmpty())
				continue;

			// 1) hashtag 테이블: normalized UNIQUE 경합 대비(두 번 조회 패턴)
			Hashtag tag = hashtagRepo.findByNormalized(norm).orElseGet(() -> {
				try {
					Hashtag ht = new Hashtag();
					ht.setTagName(raw.trim());
					ht.setNormalized(norm);
					return hashtagRepo.save(ht);
				} catch (DataIntegrityViolationException e) {
					// 다른 트랜잭션이 먼저 넣은 경우
					return hashtagRepo.findByNormalized(norm).orElseThrow();
				}
			});

			// 2) 연결 테이블 중복 방지
			if (!wltRepo.existsByListIdAndTagId(listId, tag.getId())) {
				WordListTag wlt = new WordListTag();
				wlt.setListId(listId);
				wlt.setTagId(tag.getId());
				wltRepo.save(wlt);
			}
		}
	}

	/* 모두의 단어장/내 단어장 */
	@Transactional(readOnly = true)
	public List<WordList> findAll() {
		return wordListRepo.findAll();
	}

	// WordListService.java
	@Transactional(readOnly = true)
	public List<WordList> findMine(Long ownerId, String keyword, List<String> tags) {
		logger.debug("내 단어장 검색 (multi-tag): ownerId={}, keyword={}, tags={}", ownerId, keyword, tags);

		Set<WordList> result = new LinkedHashSet<>();

		// 1) keyword(제목) 검색 (owner 한정)
		if (keyword != null && !keyword.isBlank()) {
			List<WordList> byTitle = wordListRepo.findByOwnerIdAndListNameContainingIgnoreCase(ownerId, keyword.trim());
			result.addAll(byTitle);
		}

		// 2) tags 검색 (복수, OR)
		if (tags != null && !tags.isEmpty()) {
			// 정규화: 소문자 + 불필요 문자 제거 (서비스에 이미 normalizeTag 메서드가 있으니 재사용)
			List<String> norms = tags.stream().filter(Objects::nonNull).map(String::trim).map(this::normalizeTag)
					.filter(s -> !s.isEmpty()).map(String::toLowerCase) // repository JPQL 비교가 lower(...) 사용하면 이건 선택
					.toList();

			if (!norms.isEmpty()) {
				List<WordList> byTags = wordListRepo.findByOwnerIdAndTagsNormalized(ownerId, norms);
				result.addAll(byTags);
			}
		}

		// 3) 아무 필터도 없으면 소유자 전체 반환
		if ((keyword == null || keyword.isBlank()) && (tags == null || tags.isEmpty())) {
			return wordListRepo.findByOwner_Id(ownerId);
		}

		return new ArrayList<>(result);
	}

	// WordListService.java
	@Transactional(readOnly = true)
	public Map<String, Object> getDetail(Long id) {
		Map<String, Object> map = new HashMap<>();

		WordList wl = wordListRepo.findByIdWithOwnerAndTags(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "단어장을 찾을 수 없습니다."));

		// items는 DB에서 최신순으로 가져오기
		List<WordListItem> items = itemRepo.findByWordListIdOrderByCreatedAtDesc(id);

		map.put("list", wl);
		map.put("items", items);
		map.put("listTags", wl.getTags()); // 필요하면 포함
		// (필요하면) owner 정보도 미리 꺼내서 모델에 넣을 수 있음
		map.put("ownerId", wl.getOwner() != null ? wl.getOwner().getId() : null);
		map.put("ownerNickname", wl.getOwner() != null ? wl.getOwner().getNickname() : null);

		return map;
	}

	/* 검색: 제목/닉네임/태그 */
	@Transactional(readOnly = true)
	public List<WordList> search(String title, String nickname, List<String> tags) {
		Set<WordList> resultSet = new LinkedHashSet<>();
		logger.debug("검색 시작: title={}, nickname={}, tags={}", title, nickname, tags);

		if (title != null && !title.isBlank()) {
			resultSet.addAll(wordListRepo.searchByTitle(title.trim().toLowerCase()));
		}

		if (nickname != null && !nickname.isBlank()) {
			resultSet.addAll(wordListRepo.searchByOwnerNickname(nickname.trim().toLowerCase()));
		}

		if (tags != null && !tags.isEmpty()) {
			// 정규화(소문자) 및 공백 제거
			List<String> norms = tags.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
					.map(s -> s.toLowerCase()).toList();

			if (!norms.isEmpty()) {
				// 레포지토리에 다중 조회 메서드가 있으면 한 번에 가져오기 (권장)
				try {
					List<WordList> byTags = wordListRepo.searchByTagsNormalized(norms);
					resultSet.addAll(byTags);
				} catch (Exception ex) {
					// 만약 레포지토리에 findByTagNormalizedIn 가 없으면 폴백: 태그별로 합침 (OR)
					for (String tag : norms) {
						resultSet.addAll(wordListRepo.searchByTagNormalized(tag));
					}
				}
			}
		}

		if (resultSet.isEmpty()) {
			return wordListRepo.findAll();
		}
		return new ArrayList<>(resultSet);
	}

	// 태그 정규화: 소문자 + 한글/영문/숫자만 남기고 나머지 제거
	private static final Pattern KEEP = Pattern.compile("[^0-9A-Za-z가-힣]");

	private String normalizeTag(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return "";
		}
		String s = Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase(Locale.KOREA).trim();
		s = KEEP.matcher(s).replaceAll("");
		if (s.length() > 50) {
			s = s.substring(0, 50);
		}
		return s;
	}

	/* 공식 -> 커스텀 사본 전환 (소유자만) */
	@Transactional
	public Long customizeFromOfficial(Long listItemId, Long ownerId) {
		WordListItem it = itemRepo.findById(listItemId).orElseThrow();
		if (!isOwnerOrAdmin(it.getWordList(), ownerId)) {
			throw new SecurityException("권한 없음");
		}
		if (it.getWord() == null) {
			return it.getId();
		}
		Word w = it.getWord();
		it.setCustomJapaneseWord(w.getJapaneseWord());
		it.setCustomReadingKana(w.getReadingKana());
		it.setCustomKoreanMeaning(w.getKoreanMeaning());
		it.setCustomExampleSentenceJp(w.getExampleSentenceJp());
		it.setWord(null);
		return it.getId();
	}

	@Transactional
	public void updateCustomItem(Long listItemId, Long ownerId, String jp, String kana, String kr, String ex) {
		WordListItem it = itemRepo.findById(listItemId).orElseThrow();
		if (!isOwnerOrAdmin(it.getWordList(), ownerId)) {
			throw new SecurityException("권한 없음");
		}
		if (it.getWord() != null) {
			throw new IllegalStateException("공식 단어는 직접 수정할 수 없습니다. 먼저 '내 사본으로 전환'을 해주세요.");
		}
		it.setCustomJapaneseWord(jp);
		it.setCustomReadingKana(kana);
		it.setCustomKoreanMeaning(kr);
		it.setCustomExampleSentenceJp(ex);
	}

	/* 단어장에 태그 추가 메서드 */
	@Transactional
	public void addTagsToWordList(Long listId, List<String> tagNames) {
		WordList wordList = wordListRepo.findById(listId).orElseThrow(() -> new RuntimeException("단어장이 존재하지 않습니다."));

		Set<Hashtag> tags = new HashSet<>();
		for (String tagName : tagNames) {
			String normalizedTag = normalizeTag(tagName);
			if (normalizedTag.isEmpty())
				continue; // 정규화된 태그가 빈 문자열이면 건너뜀

			Hashtag hashtag = hashtagRepo.findByNormalized(normalizedTag).orElseGet(() -> {
				Hashtag ht = new Hashtag();
				ht.setTagName(tagName);
				ht.setNormalized(normalizedTag);
				return hashtagRepo.save(ht);
			});

			tags.add(hashtag);
		}

		wordList.setTags(tags);
		wordListRepo.save(wordList);
	}

	@Transactional
	public void shareList(Long listId) {
		WordList wordList = wordListRepo.findById(listId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "단어장을 찾을 수 없습니다."));

		System.out.println("Before sharing: " + wordList.getIsShared());
		wordList.setIsShared(1);
		wordListRepo.save(wordList);
		System.out.println("After sharing: " + wordList.getIsShared());

	}

	public List<WordListItem> findItemsByListIdDesc(Long listId) {
		return itemRepo.findByWordListIdOrderByCreatedAtDesc(listId);
	}

}