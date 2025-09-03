package com.toke.toke_project.domain;
//word_list_tag 테이블 매핑.단어장과 태그의 N:N 관계 연결 테이블.
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name="word_list_tag")
@Getter @Setter @NoArgsConstructor
@IdClass(WordListTag.PK.class)
public class WordListTag {

    @Id
    @Column(name="list_id")
    private Long listId;

    @Id
    @Column(name="tag_id")
    private Long tagId;

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;

    @Data
    public static class PK implements Serializable {
        private Long listId;
        private Long tagId;
    }
}

