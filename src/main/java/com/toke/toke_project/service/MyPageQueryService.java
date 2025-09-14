package com.toke.toke_project.service;

import com.toke.toke_project.repo.QnaRepository;
import com.toke.toke_project.repo.WordListRepository;
import com.toke.toke_project.domain.QnaComment;
import com.toke.toke_project.repo.QnaCommentRepository;
import com.toke.toke_project.web.dto.QnaAccordionRow;
import com.toke.toke_project.web.dto.QnaListRow;
import com.toke.toke_project.web.dto.WordbookCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageQueryService {

    private final QnaRepository qnaRepo;
    private final WordListRepository wordListRepo;
    private final QnaCommentRepository qnaCommentRepo; // 관리자 댓글 = 답변

    /** ✅ 내 문의내역(목록 탭 기본용): 내가 쓴 글만, 요약 DTO */
    public Page<QnaListRow> findMyQnaList(Long userId, Pageable pageable) {
        return qnaRepo.pageMine(userId, pageable)
                .map(q -> new QnaListRow(
                        q.getId(),
                        q.getCategory(),
                        null, // categoryLabel: 필요 시 코드→라벨 매핑
                        q.getTitle(),
                        (q.getAuthor() != null ? q.getAuthor().getNickname() : null), // 표시이름 대체
                        q.getStatus(),
                        q.getIsSecret(),
                        q.getCreatedAt()
                ));
    }

    /** ✅ 내 단어장: 내가 만든 단어장만 (태그 포함) */
    public Page<WordbookCardDto> findMyWordbooks(Long userId, Pageable pageable) {
        return wordListRepo.findByOwnerIdOrderByCreatedAtDesc(userId, pageable)
                .map(list -> WordbookCardDto.builder()
                        .id(list.getId())
                        .title(list.getListName())
                        .description(list.getDescription())
                        .tags(list.getTags().stream()
                                .map(tag -> tag.getTagName())
                                .toList())
                        .build());
    }

    /** ✅ 내 문의내역(아코디언용): 본문 + 최신 관리자 답변 텍스트 포함 */
    public Page<QnaAccordionRow> findMyQnaAccordion(Long userId, Pageable pageable) {
        return qnaRepo.pageMine(userId, pageable)
                .map(q -> new QnaAccordionRow(
                        q.getId(),
                        q.getTitle(),
                        q.getStatus(),
                        q.getCreatedAt(),
                        q.getContent(),
                        fetchAnswerContent(q.getId()) // 관리자 댓글을 답변으로 사용
                ));
    }

    /** 최신 관리자 댓글(답변) 본문을 가져온다. 없으면 null */
    private String fetchAnswerContent(Long qnaId) {
        return qnaCommentRepo
                .findTopByQna_IdAndIsAdminOrderByCreatedAtDesc(qnaId, "Y")
                .map(QnaComment::getContent)
                .orElse(null);
    }

}
