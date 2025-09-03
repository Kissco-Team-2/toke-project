package com.toke.toke_project.service;
//관리자서비스 (비즈니스 로직) 목록/상세/등록/수정/삭제
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.web.dto.WordForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminWordService {

    private final WordRepository wordRepo;

    // 전체 목록 (검색+카테고리, 최신순)
    public List<Word> list(String q, String category) {
        return wordRepo.searchBasic(q, category);
    }

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
