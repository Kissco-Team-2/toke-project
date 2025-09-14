package com.toke.toke_project.service;

import com.toke.toke_project.domain.Qna;
import com.toke.toke_project.domain.QnaComment;
import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.QnaCommentRepository;
import com.toke.toke_project.repo.QnaRepository;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.web.dto.QnaCommentRow;
import com.toke.toke_project.web.dto.QnaDetailView;
import com.toke.toke_project.web.dto.QnaListRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepo;
    private final QnaCommentRepository qnaCommentRepo;
    private final UsersRepository usersRepo;

    /* ========= 글쓰기 ========= */
    @Transactional
    public Long write(Long userId, String category, String title, String content, boolean secret) {
        Users me = usersRepo.findById(userId).orElseThrow();

        Qna q = new Qna();
        q.setAuthor(me);
        q.setCategory(category);
        q.setTitle(title);
        q.setContent(content);
        q.setIsSecret(secret ? "Y" : "N");
        q.setStatus("OPEN");
        return qnaRepo.save(q).getId();
    }

    /* ========= 목록(비페이지) ========= */
    @Transactional(readOnly = true)
    public List<QnaListRow> listForUser(Long myId, boolean isAdmin) {
        List<Qna> rows = isAdmin
                ? qnaRepo.listAllForAdmin()
                : qnaRepo.listVisibleForUser(myId);

        return rows.stream().map(this::toListRow).toList();
    }

    /* ========= 목록(페이지) ========= */
    @Transactional(readOnly = true)
    public Page<QnaListRow> pageForUser(Long myId, boolean isAdmin, Pageable pageable) {
        Page<Qna> page = isAdmin
                ? qnaRepo.pageAllForAdmin(pageable)
                : qnaRepo.pageVisibleForUser(myId, pageable);

        return page.map(this::toListRow);
    }

    /* ========= 소유자 판별(수정버튼 노출용) ========= */
    @Transactional(readOnly = true)
    public boolean isOwner(Long qnaId, Long userId) {
        return qnaRepo.findById(qnaId)
                .map(q -> q.getAuthor() != null
                        && q.getAuthor().getId() != null
                        && q.getAuthor().getId().equals(userId))
                .orElse(false);
    }

    /* ========= 상세 보기(비밀글 접근제어 포함) ========= */
    @Transactional(readOnly = true)
    public QnaDetailView detailForUser(Long qnaId, Long myId, boolean isAdmin) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();

        if ("Y".equals(q.getIsSecret())) {
            boolean owner = q.getAuthor() != null && Objects.equals(q.getAuthor().getId(), myId);
            if (!isAdmin && !owner) {
                throw new AccessDeniedException("비밀글은 작성자/관리자만 열람할 수 있습니다.");
            }
        }
        return toDetailView(q);
    }

    /* ========= 수정 ========= */
    @Transactional
    public void update(Long qnaId, Long editorId, boolean isAdmin,
                       String category, String title, String content, boolean secret) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();

        boolean owner = q.getAuthor() != null && Objects.equals(q.getAuthor().getId(), editorId);
        if (!isAdmin && !owner) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        q.setCategory(category);
        q.setTitle(title);
        q.setContent(content);
        q.setIsSecret(secret ? "Y" : "N");
        q.setUpdatedAt(LocalDateTime.now());
    }

    /* ========= 삭제 ========= */
    @Transactional
    public void delete(Long qnaId, Long requesterId, boolean isAdmin) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();

        boolean owner = q.getAuthor() != null && Objects.equals(q.getAuthor().getId(), requesterId);
        if (!isAdmin && !owner) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        qnaCommentRepo.deleteByQna_Id(qnaId);
        qnaRepo.delete(q);
    }

    /* ========= 관리자 답변 등록(누적 저장) ========= */
    @Transactional
    public void addReply(Long qnaId, String reply, Long adminId) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();
        Users admin = usersRepo.findById(adminId).orElseThrow();

        QnaComment c = new QnaComment();
        c.setQna(q);
        c.setUser(admin);
        c.setContent(reply);
        c.setIsAdmin("Y");
        qnaCommentRepo.save(c);

        q.setStatus("ANSWERED");
 
    }

    /* ========= 댓글 목록(오래된 → 최신) ========= */
    @Transactional(readOnly = true)
    public List<QnaCommentRow> listComments(Long qnaId) {
        return qnaCommentRepo.findByQna_IdOrderByCreatedAtAsc(qnaId).stream()
                .map(c -> new QnaCommentRow(
                        c.getId(),
                        displayName(c.getUser()),
                        "Y".equals(c.getIsAdmin()),
                        c.getContent(),
                        c.getCreatedAt()
                ))
                .toList();
    }

    /* ========= DTO 매핑 ========= */
    private QnaListRow toListRow(Qna q) {
        String authorDisplay = displayName(q.getAuthor());

        String categoryLabel = switch (q.getCategory()) {
            case "WORD" -> "단어 / 표현 수정";
            case "WORDLIST" -> "단어 / 표현 추가";
            case "QUIZ" -> "신고";
            default -> "기타";
        };

        return new QnaListRow(
                q.getId(),
                q.getCategory(),
                categoryLabel,
                q.getTitle(),
                authorDisplay,
                q.getStatus(),
                q.getIsSecret(),
                q.getCreatedAt()
        );
    }

    private QnaDetailView toDetailView(Qna q) {
        String authorDisplay = displayName(q.getAuthor());


        String categoryLabel = switch (q.getCategory()) {
            case "WORD" -> "단어 / 표현 수정";
            case "WORDLIST" -> "단어 / 표현 추가";
            case "QUIZ" -> "신고";
            default -> "기타";
        };

        return new QnaDetailView(
                q.getId(),
                q.getCategory(),
                categoryLabel,
                q.getTitle(),
                q.getContent(),
                q.getStatus(),
                q.getIsSecret(),
                q.getCreatedAt(),
                authorDisplay
             
        );
    }

    /* ========= 공통 표시이름 ========= */
    private String displayName(Users u) {
        if (u == null) return null;
        if (u.getNickname() != null) return u.getNickname();
        if (u.getUsername() != null) return u.getUsername();
        return u.getEmail();
    }
}
