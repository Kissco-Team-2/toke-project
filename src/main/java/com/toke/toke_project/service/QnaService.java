package com.toke.toke_project.service;

import com.toke.toke_project.domain.Qna;
import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.QnaRepository;
import com.toke.toke_project.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaRepository qnaRepo;
    private final UsersRepository usersRepo;

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

    @Transactional(readOnly = true)
    public List<Qna> listForUser(Long myId, boolean isAdmin) {
        return isAdmin ? qnaRepo.listAllForAdmin()
                       : qnaRepo.listVisibleForUser(myId);
    }

    @Transactional(readOnly = true)
    public Qna detailForUser(Long qnaId, Long myId, boolean isAdmin) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();
        if ("Y".equals(q.getIsSecret()) && !isAdmin && !q.getAuthor().getId().equals(myId)) {
            // 비밀글: 본인/관리자만 열람
            throw new SecurityException("비밀글입니다.");
        }
        return q;
    }

    // ✅ 관리자 답변
    @Transactional
    public void addReply(Long qnaId, String reply, Long adminId) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();
        Users admin = usersRepo.findById(adminId).orElseThrow();
        q.setReply(reply);
        q.setStatus("ANSWERED");
        q.setAnsweredBy(admin);
        q.setAnsweredAt(LocalDateTime.now());
    }

    @Transactional
    public void close(Long qnaId, Long ownerId, boolean isAdmin) {
        Qna q = qnaRepo.findById(qnaId).orElseThrow();
        if (!isAdmin && !q.getAuthor().getId().equals(ownerId)) throw new SecurityException("권한 없음");
        q.setStatus("CLOSED");
    }
}
