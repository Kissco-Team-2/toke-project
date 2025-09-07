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

	
	//퀴즈 관련
	/** 카테고리에서 임의 N개(Oracle) */
	@Query(value = "SELECT * FROM word WHERE category = :category ORDER BY DBMS_RANDOM.VALUE", nativeQuery = true)
	List<Word> findRandomByCategoryOracle(@Param("category") String category, Pageable pageable);

	/** 오답 후보: 같은 카테고리, 자신 제외, 한국어 뜻 랜덤 DISTINCT */
	@Query(value = """
			SELECT DISTINCT korean_meaning
			FROM word
			WHERE category = :category
			  AND word_id <> :excludeId
			ORDER BY DBMS_RANDOM.VALUE
			""", nativeQuery = true)
	List<String> findRandomMeaningsForDistractorsOracle(@Param("category") String category,
			@Param("excludeId") Long excludeId, Pageable pageable);

	/** 오답 후보: 같은 카테고리, 자신 제외, 일본어 단어 랜덤 DISTINCT */
	@Query(value = """
			SELECT DISTINCT japanese_word
			FROM word
			WHERE category = :category
			  AND word_id <> :excludeId
			ORDER BY DBMS_RANDOM.VALUE
			""", nativeQuery = true)
	List<String> findRandomJapaneseForDistractorsOracle(@Param("category") String category,
			@Param("excludeId") Long excludeId, Pageable pageable);

}
