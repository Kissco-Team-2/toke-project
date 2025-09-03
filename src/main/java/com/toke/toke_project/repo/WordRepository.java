package com.toke.toke_project.repo;
//(DB 접근)페이지 목록 + 검색을 한 번에 처리하는 JPA 쿼리
import com.toke.toke_project.domain.Word;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    @Query("""
      SELECT w FROM Word w
      WHERE (:q IS NULL OR :q = '' 
             OR LOWER(w.japaneseWord)   LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(w.readingKana)    LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(w.koreanMeaning)  LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:cat IS NULL OR :cat = '' OR w.category = :cat)
      ORDER BY w.createdAt DESC
    """)
    List<Word> searchBasic(@Param("q") String q,
                           @Param("cat") String category);
}

