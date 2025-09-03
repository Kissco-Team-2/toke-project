package com.toke.toke_project.domain;
//DB word 테이블과 매핑
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "word")
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

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;      // DB default 시간
}
