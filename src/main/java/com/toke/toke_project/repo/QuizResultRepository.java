package com.toke.toke_project.repo;

import com.toke.toke_project.domain.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    @Query("SELECT COUNT(qr) FROM QuizResult qr WHERE qr.userId = :userId AND qr.quizId = :quizId AND qr.isCorrect = 'N'")
    Long countWrongByUserAndQuiz(@Param("userId") Long userId, @Param("quizId") Long quizId);

    @Query("SELECT MAX(qr.createdAt) FROM QuizResult qr WHERE qr.userId = :userId AND qr.quizId = :quizId AND qr.isCorrect = 'N'")
    LocalDateTime findLastWrongDateByUserAndQuiz(@Param("userId") Long userId, @Param("quizId") Long quizId);
}
