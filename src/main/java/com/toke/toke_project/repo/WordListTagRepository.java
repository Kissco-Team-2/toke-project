package com.toke.toke_project.repo;
// 단어장과 태그 연결 관계 관리 (추가/삭제)
import com.toke.toke_project.domain.WordListTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordListTagRepository extends JpaRepository<WordListTag, WordListTag.PK> {
    void deleteByListId(Long listId);

    // listId + tagId 조합이 이미 존재하는지 확인
    boolean existsByListIdAndTagId(Long listId, Long tagId);
}
