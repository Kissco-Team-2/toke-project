package com.toke.toke_project.service;

import com.toke.toke_project.domain.Word;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.web.dto.WordForm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminWordService {

    private final WordRepository wordRepo;

    // === 사용자 조회 (검색 + 정렬 + 그룹 + 페이징) ===
    public Page<Word> search(String q, String category, String mode, String group, int page, int size) {
        // 정렬 모드
        Sort sort = switch (mode) {
            case "ko" -> Sort.by("koGroup").ascending()
                             .and(Sort.by("koVowelIndex")).ascending()
                             .and(Sort.by("koreanMeaning")).ascending();
            case "ja" -> Sort.by("jaGroup").ascending()
                             .and(Sort.by("jaVowelIndex")).ascending()
                             .and(Sort.by("readingKana")).ascending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ group 필터까지 반영
        if (group != null && !group.isEmpty()) {
            if ("ko".equals(mode)) {
                return wordRepo.findByKoGroupStartingWithAndCategoryContainingAndKeyword(q, category, group, pageable);
            } else if ("ja".equals(mode)) {
                return wordRepo.findByJaGroupStartingWithAndCategoryContainingAndKeyword(q, category, group, pageable);
            }
        }

        // 기본 검색 (group 없음)
        return wordRepo.search(q, category, pageable);
    }

    // === 전체 목록 (기존 searchBasic 대체) ===
    public java.util.List<Word> list(String q, String category) {
        return wordRepo.search(q, category, Pageable.unpaged()).getContent();
    }

    // === 관리자 기능 (CRUD) ===
    public Word get(Long id) {
        return wordRepo.findById(id).orElseThrow();
    }

    @Transactional
    public Long create(WordForm f, Long adminUserId) {
        Word w = new Word();
        w.setJapaneseWord(f.getJapaneseWord());
        w.setReadingKana(f.getReadingKana());
        w.setKoreanMeaning(f.getKoreanMeaning());
        w.setCategory(f.getCategory());
        w.setExampleSentenceJp(f.getExampleSentenceJp());
        w.setCreatedBy(adminUserId);
        wordRepo.save(w);
        return w.getId();
    }

    @Transactional
    public void update(Long id, WordForm f) {
        Word w = wordRepo.findById(id).orElseThrow();
        w.setJapaneseWord(f.getJapaneseWord());
        w.setReadingKana(f.getReadingKana());
        w.setKoreanMeaning(f.getKoreanMeaning());
        w.setCategory(f.getCategory());
        w.setExampleSentenceJp(f.getExampleSentenceJp());
    }

    @Transactional
    public void delete(Long id) {
        wordRepo.deleteById(id);
    }
}
