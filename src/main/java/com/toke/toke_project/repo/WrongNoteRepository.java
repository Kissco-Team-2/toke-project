package com.toke.toke_project.repo;

import com.toke.toke_project.domain.WrongNote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WrongNoteRepository extends JpaRepository<WrongNote, Long> {

    // nested property access: user.id, quiz.quizId
    Optional<WrongNote> findByUser_IdAndQuiz_QuizId(Long userId, Long quizId);

    List<WrongNote> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // fetch user + quiz + quiz.word (if exists) to avoid lazy issues
    @Query("SELECT wn FROM WrongNote wn " +
           "JOIN FETCH wn.user u " +
           "JOIN FETCH wn.quiz q " +
           "LEFT JOIN FETCH q.word w " +
           "WHERE u.id = :userId " +
           "ORDER BY wn.createdAt DESC")
    List<WrongNote> findByUserIdWithQuiz(@Param("userId") Long userId);
    
    
    // 별표 설정(사용자 소유 검사 포함) — 직접 update로 처리하면 레이스에 대해 더 안전
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE WrongNote wn SET wn.starred = :starred WHERE wn.noteId = :noteId AND wn.user.id = :userId")
    int updateStarredByNoteIdAndUserId(@Param("noteId") Long noteId, @Param("userId") Long userId, @Param("starred") String starred);
    
    // 사용자별 starred 리스트 조회
    List<WrongNote> findByUser_IdAndStarredOrderByCreatedAtDesc(Long userId, String starred);
    
    
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE WrongNote wn " +
           "SET wn.starred = CASE WHEN wn.starred = 'Y' THEN 'N' ELSE 'Y' END " +
           "WHERE wn.noteId = :noteId AND wn.user.id = :userId")
    int toggleStarByNoteIdAndUserId(@Param("noteId") Long noteId, @Param("userId") Long userId);
    
}


