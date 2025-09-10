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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
		if (!wl.getOwner().getId().equals(ownerId))
			throw new SecurityException("권한 없음");
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
		if (!wl.getOwner().getId().equals(ownerId))
			throw new SecurityException("권한 없음");

		wl.getTags().clear(); // 중간 테이블 clean
		itemRepo.deleteByWordList_Id(listId);
		wordListRepo.delete(wl);
	}

	/* 공식 단어 추가 */
	@Transactional
	public Long addItemFromWord(Long listId, Long ownerId, Long wordId) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!wl.getOwner().getId().equals(ownerId))
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
		if (!wl.getOwner().getId().equals(ownerId)) {
			throw new SecurityException("권한 없음");
		}

		for (Long wordId : wordIds) {
			addItemAsCustomCopy(listId, ownerId, wordId);
		}
	}

	/* 커스텀 단어 추가 */
	@Transactional
	public Long addCustomItem(Long listId, Long ownerId, String jp, String kana, String kr, String ex) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!wl.getOwner().getId().equals(ownerId))
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

	@Transactional
	public Long addItemAsCustomCopy(Long listId, Long ownerId, Long wordId) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();
		if (!wl.getOwner().getId().equals(ownerId)) {
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

	/* 항목 삭제(본인만) */
	@Transactional
	public void removeItem(Long listItemId, Long ownerId) {
		WordListItem it = itemRepo.findById(listItemId).orElseThrow();
		if (!it.getWordList().getOwner().getId().equals(ownerId))
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

	@Transactional(readOnly = true)
	public List<WordList> findMine(Long ownerId, String keyword, String tag) {
		logger.debug("내 단어장 검색: ownerId={}, keyword={}, tag={}", ownerId, keyword, tag);

		if (tag != null && !tag.isBlank()) {
			return wordListRepo.findByOwnerIdAndTagNormalized(ownerId, tag.toLowerCase());
		}
		if (keyword != null && !keyword.isBlank()) {
			return wordListRepo.findByOwnerIdAndListNameContainingIgnoreCase(ownerId, keyword.toLowerCase());
		}
		return wordListRepo.findByOwner_Id(ownerId);
	}

	/* 단어장 상세 + 항목 */
	@Transactional(readOnly = true)
	public Map<String, Object> getDetail(Long listId) {
		WordList wl = wordListRepo.findById(listId).orElseThrow();

		// tags 강제 초기화
		wl.getTags().size();

		List<WordListItem> items = itemRepo.findByWordList_IdOrderByIdAsc(listId);

		// 공식 단어 word 강제 초기화
		for (WordListItem it : items) {
			if (it.getWord() != null) {
				it.getWord().getJapaneseWord(); // 접근해서 초기화
				it.getWord().getKoreanMeaning();
				it.getWord().getReadingKana();
				it.getWord().getExampleSentenceJp();
			}
		}

		Map<String, Object> res = new HashMap<>();
		res.put("list", wl);
		res.put("items", items);
		return res;
	}

	/* 검색: 제목/닉네임/태그 */
	@Transactional(readOnly = true)
	public List<WordList> search(String title, String nickname, String tag) {
		Set<WordList> resultSet = new LinkedHashSet<>();
		logger.debug("검색 시작: title={}, nickname={}, tag={}", title, nickname, tag);

		if (title != null && !title.isBlank()) {
			resultSet.addAll(wordListRepo.searchByTitle(title.trim().toLowerCase()));
		}

		if (nickname != null && !nickname.isBlank()) {
			resultSet.addAll(wordListRepo.searchByOwnerNickname(nickname.trim().toLowerCase()));
		}

		if (tag != null && !tag.isBlank()) {
			resultSet.addAll(wordListRepo.searchByTagNormalized(tag.toLowerCase()));
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
		// 소유자 체크
		if (!it.getWordList().getOwner().getId().equals(ownerId)) {
			throw new SecurityException("권한 없음");
		}
		if (it.getWord() == null) {
			// 이미 커스텀인 경우는 그대로 반환
			return it.getId();
		}
		// 공식 단어를 커스텀 필드로 복사하고, 공식 참조를 끊는다
		Word w = it.getWord();
		it.setCustomJapaneseWord(w.getJapaneseWord());
		it.setCustomReadingKana(w.getReadingKana());
		it.setCustomKoreanMeaning(w.getKoreanMeaning());
		it.setCustomExampleSentenceJp(w.getExampleSentenceJp());
		it.setWord(null); // 공식 참조 해제 → 이제 완전 커스텀 항목
		// 필요 시 category를 커스텀에도 보관하고 싶다면 WLI에 custom_category 컬럼을 추가하는 방식도 가능
		return it.getId();
	}

	@Transactional
	public void updateCustomItem(Long listItemId, Long ownerId, String jp, String kana, String kr, String ex) {
		WordListItem it = itemRepo.findById(listItemId).orElseThrow();
		if (!it.getWordList().getOwner().getId().equals(ownerId)) {
			throw new SecurityException("권한 없음");
		}
		// 커스텀만 수정 허용 (공식 연결 상태면 먼저 customizeFromOfficial 호출 유도)
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

}
