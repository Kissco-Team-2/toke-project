package com.toke.toke_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "qna_comment")
@Getter @Setter
@SequenceGenerator(name = "qnaCommentSeq", sequenceName = "seq_qna_comment_id", allocationSize = 1)
public class QnaComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qnaCommentSeq")
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private Qna qna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "is_admin", length = 1, nullable = false)
    private String isAdmin = "N"; // 'Y' or 'N'

    // DB에서 DEFAULT SYSTIMESTAMP 사용하므로 insertable=false 로 둬야 JPA가 null 안 넣음
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
