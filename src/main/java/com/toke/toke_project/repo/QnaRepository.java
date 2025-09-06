package com.toke.toke_project.repo;

import com.toke.toke_project.domain.Qna;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaRepository extends JpaRepository<Qna, Long> {

    // 공개글 + 내가 쓴 비밀글 포함 목록
    @Query("""
      select q from Qna q 
      where q.isSecret='N' or q.author.id = :myId
      order by q.createdAt desc
    """)
    List<Qna> listVisibleForUser(Long myId);

    // 관리자: 전체
    @Query("select q from Qna q order by q.createdAt desc")
    List<Qna> listAllForAdmin();
}
