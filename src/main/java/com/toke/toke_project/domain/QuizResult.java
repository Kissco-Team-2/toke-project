package com.toke.toke_project.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "quiz_result")
public class QuizResult {
	@Id
	@SequenceGenerator(name = "quiz_result_seq", sequenceName = "seq_result_id", // 👈 DB 시퀀스명과 일치
			allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quiz_result_seq")
	@Column(name = "result_id")
	private Long resultId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "word_id", nullable = false)
	private Long wordId;

	@Column(name = "user_answer", length = 1)
	private String userAnswer; // A/B/C/D (무응답이면 null 허용)

	@Column(name = "is_correct", length = 1, nullable = false)
	private String isCorrect; // Y/N

	@Column(name = "created_at")
	private LocalDateTime createdAt;

}
