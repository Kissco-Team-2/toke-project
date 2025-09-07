package com.toke.toke_project.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "quiz")
public class Quiz {

	@Id
	@SequenceGenerator(
	    name = "quiz_seq",              // JPA 내부에서 부를 별칭
	    sequenceName = "seq_quiz_id",   // DB 실제 시퀀스 이름과 일치시켜야 함
	    allocationSize = 1
	)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "quiz_seq")
	@Column(name = "quiz_id")
	private Long quizId;

	@Column(name = "question_type")
	private String questionType; // "JP_TO_KR" or "KR_TO_JP"

	@Column(name = "question")
	private String question;

	@Column(name = "option_a")
	private String optionA;

	@Column(name = "option_b")
	private String optionB;

	@Column(name = "option_c")
	private String optionC;

	@Column(name = "option_d")
	private String optionD;

	@Column(name = "correct_answer")
	private String correctAnswer; // "A"/"B"/"C"/"D"

	@Column(name = "created_by")
	private Long createdBy;

	@Column(name = "created_at", updatable = false, insertable = false)
	private LocalDateTime createdAt;

}
