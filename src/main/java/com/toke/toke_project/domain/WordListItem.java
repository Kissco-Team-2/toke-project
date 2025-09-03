package com.toke.toke_project.domain;
//word_list_item 테이블 매핑. 단어장 안에 들어가는 단어 항목.
//Word 테이블과 연결하거나, 커스텀 단어(사용자 입력) 저장 가능.
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_list_item")
@Getter @Setter @NoArgsConstructor
public class WordListItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "listItemSeq")
    @SequenceGenerator(name = "listItemSeq", sequenceName = "seq_list_item_id", allocationSize = 1)
    @Column(name = "list_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="list_id", nullable = false)
    private WordList wordList;

    // 공식 단어로 추가할 때
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="word_id")
    private Word word;

    // 커스텀 입력으로 추가할 때
    @Column(name="custom_japanese_word", length = 100)
    private String customJapaneseWord;

    @Column(name="custom_reading_kana", length = 300)
    private String customReadingKana;

    @Column(name="custom_korean_meaning", length = 255)
    private String customKoreanMeaning;

    @Column(name="custom_example_sentence_jp", length = 1000)
    private String customExampleSentenceJp;

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;
}

