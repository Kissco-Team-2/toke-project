package com.toke.toke_project.domain;
//hashtag 테이블 매핑. 단어장 검색을 위한 태그 정보.
//tag_name(보여줄 원본)과 normalized(검색용) 둘 다 저장.
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hashtag")
@Getter @Setter @NoArgsConstructor
public class Hashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tagSeq")
    @SequenceGenerator(name="tagSeq", sequenceName="seq_tag_id", allocationSize=1)
    @Column(name="tag_id")
    private Long id;

    @Column(name="tag_name", nullable = false, length = 50)
    private String tagName;       // 표시용

    @Column(name="normalized", nullable = false, length = 50, unique = true)
    private String normalized;    // 검색용(소문자/공백제거/기호제거)

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;
}

