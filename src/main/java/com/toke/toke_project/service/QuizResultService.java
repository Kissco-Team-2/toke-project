package com.toke.toke_project.service;

import com.toke.toke_project.domain.QuizResult;
import com.toke.toke_project.repo.QuizResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * QuizResult 저장 책임을 분리한 서비스.
 * - quiz_result 저장
 * - 저장 후 틀린 문제(N)일 경우 WrongNoteService에 오답노트 등록을 위임
 */
@Service
public class QuizResultService {

    private final QuizResultRepository quizResultRepository;
    private final WrongNoteService wrongNoteService;

    public QuizResultService(QuizResultRepository quizResultRepository,
                             WrongNoteService wrongNoteService) {
        this.quizResultRepository = quizResultRepository;
        this.wrongNoteService = wrongNoteService;
    }

    /**
     * QuizResult를 저장하고, 틀린 경우 오답노트 등록을 시도한다.
     * registerIfNotExists 내부에서 존재 여부 체크하므로 중복 생성은 방지된다.
     *
     * 트랜잭션 안에서 quiz_result 저장과 오답노트 등록 시도의 원자성을 보장한다.
     */
    @Transactional
    public QuizResult saveResult(QuizResult result) {
        QuizResult saved = quizResultRepository.save(result);

        if ("N".equals(saved.getIsCorrect())) {
            wrongNoteService.recordWrong(saved.getUserId(), saved.getWordId()); // ✅ 이거 하나만
        }

        return saved;
    }

}
