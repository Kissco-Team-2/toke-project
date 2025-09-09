package com.toke.toke_project.repo;

import com.toke.toke_project.domain.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    /* ====================== 검색(목록) ====================== */
    @Query("""
            SELECT w FROM Word w
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(w.japaneseWord)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(w.readingKana)   LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(w.koreanMeaning) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:category IS NULL OR :category = '' OR w.category = :category)
            """)
    Page<Word> search(@Param("q") String q, @Param("category") String category, Pageable pageable);

    // koGroup 필터
    @Query("""
              SELECT w FROM Word w
              WHERE (:q IS NULL OR :q = ''
                     OR LOWER(w.japaneseWord)  LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(w.readingKana)   LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(w.koreanMeaning) LIKE LOWER(CONCAT('%', :q, '%')))
                AND (:category IS NULL OR :category = '' OR w.category = :category)
                AND w.koGroup = :group
            """)
    Page<Word> findByKoGroupStartingWithAndCategoryContainingAndKeyword(@Param("q") String q,
                                                                        @Param("category") String category,
                                                                        @Param("group") String group,
                                                                        Pageable pageable);

    // jaGroup 필터
    @Query("""
              SELECT w FROM Word w
              WHERE (:q IS NULL OR :q = ''
                     OR LOWER(w.japaneseWord)  LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(w.readingKana)   LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(w.koreanMeaning) LIKE LOWER(CONCAT('%', :q, '%')))
                AND (:category IS NULL OR :category = '' OR w.category = :category)
                AND w.jaGroup = :group
            """)
    Page<Word> findByJaGroupStartingWithAndCategoryContainingAndKeyword(@Param("q") String q,
                                                                        @Param("category") String category,
                                                                        @Param("group") String group,
                                                                        Pageable pageable);

    /* ====================== 퀴즈 랜덤 픽 ====================== */
    /** 전체에서 임의 N개(Oracle) */
    @Query(value = """
        SELECT * FROM (
          SELECT * FROM word
          ORDER BY DBMS_RANDOM.VALUE
        ) WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Word> findRandomOracle(@Param("limit") int limit);

    /** 카테고리에서 임의 N개 — 공백/전각 공백/양끝 공백을 무시하고 비교 */
    @Query(value = """
        SELECT * FROM (
          SELECT * FROM word
           WHERE TRIM(REGEXP_REPLACE(category, '\\s+', '')) =
                 TRIM(REGEXP_REPLACE(:category, '\\s+', ''))
           ORDER BY DBMS_RANDOM.VALUE
        ) WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Word> findRandomByCategoryOracleFlex(@Param("category") String category,
                                              @Param("limit") int limit);

    /** 오답 후보(한글 뜻) — 동일한 유연 비교 */
    @Query(value = """
        SELECT * FROM (
          SELECT DISTINCT korean_meaning
            FROM word
           WHERE TRIM(REGEXP_REPLACE(category, '\\s+', '')) =
                 TRIM(REGEXP_REPLACE(:category, '\\s+', ''))
             AND word_id <> :excludeId
           ORDER BY DBMS_RANDOM.VALUE
        ) WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<String> findRandomMeaningsForDistractorsOracle(
            @Param("category") String category,
            @Param("excludeId") Long excludeId,
            @Param("limit") int limit);

    /** 오답 후보(일본어 표기) — 동일한 유연 비교 */
    @Query(value = """
        SELECT * FROM (
          SELECT DISTINCT japanese_word
            FROM word
           WHERE TRIM(REGEXP_REPLACE(category, '\\s+', '')) =
                 TRIM(REGEXP_REPLACE(:category, '\\s+', ''))
             AND word_id <> :excludeId
           ORDER BY DBMS_RANDOM.VALUE
        ) WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<String> findRandomJapaneseForDistractorsOracle(
            @Param("category") String category,
            @Param("excludeId") Long excludeId,
            @Param("limit") int limit);

    /* ====================== 안전한 단건 조회(중복 허용) ====================== */
    // 중복 가능하므로 Optional<Word> 대신 List<Word>로 받는다.
    List<Word> findByJapaneseWord(String japaneseWord);
    List<Word> findByKoreanMeaning(String koreanMeaning);

    // OR 조건 다건 조회 (정렬 기준은 필요에 맞게 변경 가능)
    @Query("""
           select w
           from Word w
           where w.japaneseWord = :text or w.koreanMeaning = :text
           order by w.id asc
           """)
    List<Word> findAllByJapaneseOrKorean(@Param("text") String text);

    /** 아무거나 1개만 Optional로 (중복 존재해도 안전) */
    default Optional<Word> findAnyByJapaneseOrKorean(String text) {
        List<Word> list = findAllByJapaneseOrKorean(text);
        return (list == null || list.isEmpty()) ? Optional.empty() : Optional.of(list.get(0));
    }

    /** 호환용: 기존 이름 유지. 내부적으로 안전 메서드 사용 */
    default Optional<Word> findByJapaneseOrKorean(String text) {
        return findAnyByJapaneseOrKorean(text);
    }
}
