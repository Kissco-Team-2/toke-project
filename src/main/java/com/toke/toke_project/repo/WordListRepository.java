package com.toke.toke_project.repo;
//단어장 목록/소유자별/검색(제목, 닉네임, 태그) 조회.
import com.toke.toke_project.domain.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface WordListRepository extends JpaRepository<WordList, Long> {
    List<WordList> findByOwner_Id(Long userId);

    // 제목 검색(부분일치, 대소문자 무시)
    @Query("select w from WordList w where lower(w.listName) like lower(concat('%', ?1, '%'))")
    List<WordList> searchByTitle(String keyword);

    // 닉네임으로 검색
    @Query("select w from WordList w where lower(w.owner.nickname) like lower(concat('%', ?1, '%'))")
    List<WordList> searchByOwnerNickname(String nickname);

    // 태그 검색
    @Query("""
      select wl from WordList wl
      where wl.id in (
        select wlt.listId from WordListTag wlt
        join Hashtag ht on ht.id = wlt.tagId
        where ht.normalized = ?1
      )
    """)
    List<WordList> searchByTagNormalized(String normalized);
}

