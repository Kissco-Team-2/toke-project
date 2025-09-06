package com.toke.toke_project.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity @Table(name = "qna")
@Getter @Setter
@SequenceGenerator(name="qnaSeq", sequenceName="seq_qna_id", allocationSize=1)
public class Qna {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "qnaSeq")
    @Column(name = "qna_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 작성자
    @JoinColumn(name = "user_id", nullable = false)
    private Users author;

    @Column(nullable = false, length = 20)  // WORD/QUIZ/WORDLIST/OTHER
    private String category;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";  // OPEN / ANSWERED / CLOSED

    @Column(name="is_secret", length=1)
    private String isSecret = "N";   // 'Y'/'N'

    @Lob
    @Column(name="reply")
    private String reply;            // 관리자 답변

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private Users answeredBy;        // 답변 관리자

    @Column(name="answered_at")
    private LocalDateTime answeredAt;

    @Column(name="created_at", insertable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;
}
