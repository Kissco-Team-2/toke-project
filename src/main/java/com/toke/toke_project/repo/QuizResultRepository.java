package com.toke.toke_project.repo;

import com.toke.toke_project.domain.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

	@Query("SELECT COUNT(qr) FROM QuizResult qr WHERE qr.userId = :userId AND qr.wordId = :wordId AND qr.isCorrect = 'N'")
	Long countWrongByUserAndWord(@Param("userId") Long userId, @Param("wordId") Long wordId);

	@Query("SELECT MAX(qr.createdAt) FROM QuizResult qr WHERE qr.userId = :userId AND qr.wordId = :wordId AND qr.isCorrect = 'N'")
	LocalDateTime findLastWrongDateByUserAndWord(@Param("userId") Long userId, @Param("wordId") Long wordId);
}
