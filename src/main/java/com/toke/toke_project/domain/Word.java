package com.toke.toke_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.toke.toke_project.util.HangulUtil;
import com.toke.toke_project.util.KanaUtil;

@Entity
@Table(name = "word")
@Getter @Setter
@SequenceGenerator(name="wordSeq", sequenceName="seq_word_id", allocationSize = 1)
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wordSeq")
    @Column(name="word_id")
    private Long id;

    @Column(name="japanese_word", nullable=false, length=300)
    private String japaneseWord;          // 일본어 원문(한자/가나)

    @Column(name="reading_kana", length=300)
    private String readingKana;           // 후리가나(히라/가타카나)

    @Column(name="korean_meaning", nullable=false, length=300)
    private String koreanMeaning;         // 한국어 뜻(필수)

    @Column(length=200)
    private String category;              // 카테고리(옵션)

    @Column(name="example_sentence_jp", length=1000)
    private String exampleSentenceJp;     // 일본어 예문(옵션)

    @Column(name="created_by", nullable=false)
    private Long createdBy;               // 등록 관리자 ID

    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;      // 등록 시간 (엔티티 생성 시 자동 세팅)

    // ===== 정렬/분류용 컬럼 =====
    @Column(name="ko_group", length=1)
    private String koGroup;               // 한글 대분류 (가,나,다…하)

    @Column(name="ko_vowel_index")
    private Integer koVowelIndex;         // 한글 모음 index (0~20)

    @Column(name="ja_group", length=1)
    private String jaGroup;               // 일본어 대분류 (あ,か,さ…わ)

    @Column(name="ja_vowel_index")
    private Integer jaVowelIndex;         // 일본어 모음 index (0~4)

    // ===== 자동 채움 =====
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        fillIndexes();
    }

    @PreUpdate
    public void recalcIndexes() {
        fillIndexes();
    }

    private void fillIndexes() {
        this.koGroup = HangulUtil.groupOf(this.koreanMeaning);
        this.koVowelIndex = HangulUtil.vowelIndexOf(this.koreanMeaning);

        this.jaGroup = KanaUtil.groupOf(this.readingKana);
        this.jaVowelIndex = KanaUtil.vowelIndexOf(this.readingKana);
    }
}
