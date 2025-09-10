package com.toke.toke_project.domain;
//word_list 테이블 매핑. 단어장 기본 정보(제목, 설명, 소유자, 생성일).
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    
 // Many-to-Many 관계 설정: 단어장과 태그의 관계를 매핑
    @ManyToMany(fetch = FetchType.EAGER) // Eager Loading으로 변경
    @JoinTable(
        name = "word_list_tag", 
        joinColumns = @JoinColumn(name = "list_id"), 
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Hashtag> tags = new HashSet<>();

    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

