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

    /* 단어장 생성 */
    @Transactional
    public Long createList(Long ownerId, String name, String desc, List<String> tags) {
        Users owner = usersRepo.findById(ownerId).orElseThrow();
        WordList wl = new WordList();
        wl.setOwner(owner);
        wl.setListName(name);
        wl.setDescription(desc);
        wordListRepo.save(wl);

        if (tags != null) attachTags(wl.getId(), tags);
        return wl.getId();
    }

    /* 단어장 수정(본인만) */
    @Transactional
    public void updateList(Long listId, Long ownerId, String name, String desc, List<String> tags) {
        WordList wl = wordListRepo.findById(listId).orElseThrow();
        if (!wl.getOwner().getId().equals(ownerId)) throw new SecurityException("권한 없음");
        if (name != null) wl.setListName(name);
        if (desc != null) wl.setDescription(desc);

        if (tags != null) {
            wltRepo.deleteByListId(listId); // 기존 태그 전부 제거 후
            attachTags(listId, tags);       // 새 태그 부착
        }
    }

    /* 단어장 삭제(본인만) */
    @Transactional
    public void deleteList(Long listId, Long ownerId) {
        WordList wl = wordListRepo.findById(listId).orElseThrow();
        if (!wl.getOwner().getId().equals(ownerId)) throw new SecurityException("권한 없음");
        wltRepo.deleteByListId(listId);
        itemRepo.deleteByWordList_Id(listId);
        wordListRepo.deleteById(listId);
    }

    /* 공식 단어 추가 */
    @Transactional
    public Long addItemFromWord(Long listId, Long ownerId, Long wordId) {
        WordList wl = wordListRepo.findById(listId).orElseThrow();
        if (!wl.getOwner().getId().equals(ownerId)) throw new SecurityException("권한 없음");
        Word w = wordRepo.findById(wordId).orElseThrow();

        WordListItem it = new WordListItem();
        it.setWordList(wl);
        it.setWord(w);
        itemRepo.save(it);
        return it.getId();
    }

    /* 커스텀 단어 추가 */
    @Transactional
    public Long addCustomItem(Long listId, Long ownerId,
                              String jp, String kana, String kr, String ex) {
        WordList wl = wordListRepo.findById(listId).orElseThrow();
        if (!wl.getOwner().getId().equals(ownerId)) throw new SecurityException("권한 없음");

        WordListItem it = new WordListItem();
        it.setWordList(wl);
        it.setCustomJapaneseWord(jp);
        it.setCustomReadingKana(kana);
        it.setCustomKoreanMeaning(kr);
        it.setCustomExampleSentenceJp(ex);
        itemRepo.save(it);
        return it.getId();
    }

    /* 항목 삭제(본인만) */
    @Transactional
    public void removeItem(Long listItemId, Long ownerId) {
        WordListItem it = itemRepo.findById(listItemId).orElseThrow();
        if (!it.getWordList().getOwner().getId().equals(ownerId)) throw new SecurityException("권한 없음");
        itemRepo.delete(it);
    }

    /* 태그 부착: normalized 중복 방지 + UNIQUE 경합 대응 + 연결 중복 방지 */
    @Transactional
    public void attachTags(Long listId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;

        for (String raw : tags) {
            String norm = normalizeTag(raw);
            if (norm.isEmpty()) continue;

            // 1) hashtag 테이블: normalized UNIQUE 경합 대비(두 번 조회 패턴)
            Hashtag tag = hashtagRepo.findByNormalized(norm)
                .orElseGet(() -> {
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
    public List<WordList> findAll() { return wordListRepo.findAll(); }

    @Transactional(readOnly = true)
    public List<WordList> findMine(Long ownerId) { return wordListRepo.findByOwner_Id(ownerId); }

    /* 단어장 상세 + 항목 */
    @Transactional(readOnly = true)
    public Map<String,Object> getDetail(Long listId) {
        WordList wl = wordListRepo.findById(listId).orElseThrow();
        List<WordListItem> items = itemRepo.findByWordList_IdOrderByIdAsc(listId);
        Map<String,Object> res = new HashMap<>();
        res.put("list", wl);
        res.put("items", items);
        return res;
    }

    /* 검색: 제목/닉네임/태그 */
    @Transactional(readOnly = true)
    public List<WordList> search(String title, String nickname, String tag) {
        Set<WordList> out = new LinkedHashSet<>();
        if (title != null && !title.isBlank()) out.addAll(wordListRepo.searchByTitle(title.trim()));
        if (nickname != null && !nickname.isBlank()) out.addAll(wordListRepo.searchByOwnerNickname(nickname.trim()));
        if (tag != null && !tag.isBlank()) out.addAll(wordListRepo.searchByTagNormalized(normalizeTag(tag)));
        if (out.isEmpty()) return wordListRepo.findAll();
        return new ArrayList<>(out);
    }

    /* 태그 정규화: 소문자 + 한글/영문/숫자만 남기고 나머지 제거 */
    private static final Pattern KEEP = Pattern.compile("[^0-9A-Za-z가-힣]");
    private String normalizeTag(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                              .toLowerCase(Locale.KOREA)
                              .trim();
        s = KEEP.matcher(s).replaceAll("");
        // DB normalized 컬럼이 VARCHAR2(50)이므로 안전하게 컷(선택)
        if (s.length() > 50) s = s.substring(0, 50);
        return s;
    }
}
