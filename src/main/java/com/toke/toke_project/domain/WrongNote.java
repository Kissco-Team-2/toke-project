package com.toke.toke_project.domain;


import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "wrong_note")
public class WrongNote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_note_gen")
    @SequenceGenerator(name = "seq_note_gen", sequenceName = "seq_note_id", allocationSize = 1)
    @Column(name = "note_id")
    private Long noteId;

    // user_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // quiz_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Lob
    @Column(name = "note")
    private String note;

    @Column(name = "starred", length = 1)
    private String starred = "N";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WrongNote() { }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (starred == null) starred = "N";
    }


}
