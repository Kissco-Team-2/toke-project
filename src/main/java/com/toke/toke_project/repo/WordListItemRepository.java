package com.toke.toke_project.repo;

//단어장 안의 항목 목록 조회 & 삭제.
import com.toke.toke_project.domain.WordListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordListItemRepository extends JpaRepository<WordListItem, Long> {
	@Query("SELECT i FROM WordListItem i LEFT JOIN FETCH i.word WHERE i.wordList.id = :listId ORDER BY i.id ASC")
	List<WordListItem> findByWordList_IdOrderByIdAsc(@Param("listId") Long listId);

	void deleteByWordList_Id(Long listId);
}