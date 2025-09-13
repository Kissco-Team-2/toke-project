package com.toke.toke_project.repo;
//단어장 목록/소유자별/검색(제목, 닉네임, 태그) 조회.
import com.toke.toke_project.domain.WordList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordListRepository extends JpaRepository<WordList, Long> {
    
	
	//소유자 ID로 조회
	List<WordList> findByOwner_Id(Long userId);

	   // 제목 검색 (부분일치, 대소문자 무시)
    @Query("select w from WordList w where lower(w.listName) like lower(concat('%', :keyword, '%'))")
    List<WordList> searchByTitle(@Param("keyword") String keyword);

    // 단어장 ID로 태그까지 fetch join
    @Query("select distinct wl from WordList wl left join fetch wl.tags where wl.id = :id")
    Optional<WordList> findByIdWithTags(@Param("id") Long id);

    // 닉네임 검색
    @Query("select w from WordList w where lower(w.owner.nickname) like lower(concat('%', :nickname, '%'))")
    List<WordList> searchByOwnerNickname(@Param("nickname") String nickname);

    // 전체 단어장: 태그 검색
    @Query("""
           select distinct wl
           from WordList wl
           join wl.tags t
           where lower(t.normalized) = lower(:normalized)
           """)
    List<WordList> searchByTagNormalized(@Param("normalized") String normalized);

    // 내 단어장: 제목 검색
    @Query("""
           select wl from WordList wl
           where wl.owner.id = :ownerId
             and lower(wl.listName) like lower(concat('%', :keyword, '%'))
           """)
    List<WordList> findByOwnerIdAndListNameContainingIgnoreCase(@Param("ownerId") Long ownerId,
                                                                @Param("keyword") String keyword);

    // ✅ 내 단어장: 태그 검색
    @Query("""
           select distinct wl
           from WordList wl
           join wl.tags t
           where wl.owner.id = :ownerId
             and lower(t.normalized) = lower(:normalized)
           """)
    List<WordList> findByOwnerIdAndTagNormalized(@Param("ownerId") Long ownerId,
                                                 @Param("normalized") String normalized);
    
    List<WordList> findByIsSharedTrue();
    
    @Query("SELECT w FROM WordList w " +
            "LEFT JOIN FETCH w.owner o " +        // owner 미리 로드
            "LEFT JOIN FETCH w.tags t " +         // 필요하면 tags도 미리 로드
            "WHERE w.id = :id")
     Optional<WordList> findByIdWithOwnerAndTags(@Param("id") Long id);
}

