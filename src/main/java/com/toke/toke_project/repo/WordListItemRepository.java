package com.toke.toke_project.repo;
//단어장 안의 항목 목록 조회 & 삭제.
import com.toke.toke_project.domain.WordListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WordListItemRepository extends JpaRepository<WordListItem, Long> {
    List<WordListItem> findByWordList_IdOrderByIdAsc(Long listId);
    void deleteByWordList_Id(Long listId);
}

