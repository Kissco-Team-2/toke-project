package com.toke.toke_project.repo;

import com.toke.toke_project.domain.Qna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    /* ===== 목록(비페이지) ===== */

    /** 관리자: 전부 (최신순) */
    @Query("""
           select q
             from Qna q
            order by q.id desc
           """)
    List<Qna> listAllForAdmin();

    /** 일반사용자: 비밀글이 아니거나 내 글 (최신순) */
    @Query("""
           select q
             from Qna q
            where q.isSecret = 'N'
               or q.author.id = :myId
            order by q.id desc
           """)
    List<Qna> listVisibleForUser(@Param("myId") Long myId);

    /* ===== 목록(페이지) ===== */

    /** 관리자: 전부 페이징 */
    @Query("""
           select q
             from Qna q
            order by q.id desc
           """)
    Page<Qna> pageAllForAdmin(Pageable pageable);

    /** 일반사용자: 비밀글이 아니거나 내 글 페이징 */
    @Query("""
           select q
             from Qna q
            where q.isSecret = 'N'
               or q.author.id = :myId
            order by q.id desc
           """)
    Page<Qna> pageVisibleForUser(@Param("myId") Long myId, Pageable pageable);
    
    /** 내가 쓴 글만 페이징 */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "author")
    @Query(
      value = """
              select q
                from Qna q
               where q.author.id = :userId
               order by q.createdAt desc
              """,
      countQuery = """
              select count(q)
                from Qna q
               where q.author.id = :userId
              """
    )
    Page<Qna> pageMine(@Param("userId") Long userId, Pageable pageable);
}
