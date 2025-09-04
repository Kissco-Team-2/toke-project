package com.toke.toke_project.repo;

import com.toke.toke_project.domain.Word;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
			  AND (:category IS NULL OR :category = '' OR w.category = :category)
			""")
	Page<Word> search(@Param("q") String q, @Param("category") String category, Pageable pageable);

	// koGroup 필터
	@Query("""
			  SELECT w FROM Word w
			  WHERE (:q IS NULL OR :q = ''
			         OR LOWER(w.japaneseWord) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.readingKana) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.koreanMeaning) LIKE LOWER(CONCAT('%', :q, '%')))
			    AND (:category IS NULL OR :category = '' OR w.category = :category)
			    AND w.koGroup = :group
			""")
	Page<Word> findByKoGroupStartingWithAndCategoryContainingAndKeyword(@Param("q") String q,
			@Param("category") String category, @Param("group") String group, Pageable pageable);

	// jaGroup 필터
	@Query("""
			  SELECT w FROM Word w
			  WHERE (:q IS NULL OR :q = ''
			         OR LOWER(w.japaneseWord) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.readingKana) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.koreanMeaning) LIKE LOWER(CONCAT('%', :q, '%')))
			    AND (:category IS NULL OR :category = '' OR w.category = :category)
			    AND w.jaGroup = :group
			""")
	Page<Word> findByJaGroupStartingWithAndCategoryContainingAndKeyword(@Param("q") String q,
			@Param("category") String category, @Param("group") String group, Pageable pageable);

}
