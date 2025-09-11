package com.toke.toke_project.repo;

import com.toke.toke_project.domain.QnaComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaCommentRepository extends JpaRepository<QnaComment, Long> {

    /** 특정 QnA의 댓글(오래된 순) */
    List<QnaComment> findByQna_IdOrderByCreatedAtAsc(Long qnaId);

    /** 특정 QnA의 모든 댓글 삭제 */
    void deleteByQna_Id(Long qnaId);

    /** 최신 댓글 1건 */
    QnaComment findTopByQna_IdOrderByCreatedAtDesc(Long qnaId);
}
