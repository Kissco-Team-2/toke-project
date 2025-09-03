package com.toke.toke_project.domain;
//word_list 테이블 매핑. 단어장 기본 정보(제목, 설명, 소유자, 생성일).
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_list")
@Getter @Setter @NoArgsConstructor
public class WordList {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "listSeq")
    @SequenceGenerator(name = "listSeq", sequenceName = "seq_list_id", allocationSize = 1)
    @Column(name = "list_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 만든 사람
    @JoinColumn(name = "user_id", nullable = false)
    private Users owner;

    @Column(name="list_name", nullable = false, length = 100)
    private String listName;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

