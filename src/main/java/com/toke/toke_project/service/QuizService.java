package com.toke.toke_project.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.toke.toke_project.domain.Quiz;
import com.toke.toke_project.domain.QuizResult;
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.repo.QuizRepository;
import com.toke.toke_project.repo.QuizResultRepository;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.service.model.*;
import com.toke.toke_project.web.dto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Quiz 관련 서비스: 생성(관리자), 채점(사용자) — 채점 시 문항별 상세 결과 반환 및 quiz_result 저장
 */
@Service
public class QuizService {

    private final WordRepository wordRepo;
    private final QuizRepository quizRepo;
    private final QuizResultRepository quizResultRepo;
    private final Cache<String, QuizPaper> quizCache;

    public QuizService(WordRepository wordRepo,
                       QuizRepository quizRepo,
                       QuizResultRepository quizResultRepo,
                       Cache<String, QuizPaper> quizCache) {
        this.wordRepo = wordRepo;
        this.quizRepo = quizRepo;
        this.quizResultRepo = quizResultRepo;
        this.quizCache = quizCache;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public QuizView generate(GenerateRequest req) {
        String category = req.category();
        QuestionMode mode = (req.mode() == null) ? QuestionMode.JP_TO_KR : req.mode();
        int n = (req.questionCount() == null || req.questionCount() <= 0) ? 10 : req.questionCount();

        // 카테고리 랜덤 N개(Oracle)
        List<Word> picked = wordRepo.findRandomByCategoryOracle(category, PageRequest.of(0, n));
        if (picked.size() < n)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 카테고리에 문제가 부족합니다.");

        List<QuizQuestion> questions = new ArrayList<>();

        for (Word w : picked) {
            final String prompt;
            final String correctText;

            if (mode == QuestionMode.JP_TO_KR) {
                prompt = "'" + w.getJapaneseWord() + "'란 무슨 뜻입니까?";
                correctText = w.getKoreanMeaning();
            } else {
                prompt = "'" + w.getKoreanMeaning() + "'에 해당하는 일본어는?";
                correctText = w.getJapaneseWord();
            }

            List<String> options = buildOptions(category, w, mode, correctText);
            int answerIndex = options.indexOf(correctText);
            String correctKey = indexToKey(answerIndex);

            // 1) quiz 테이블 저장 → quiz_id 확보
            Quiz q = new Quiz();
            q.setQuestionType(mode.name());
            q.setQuestion(prompt);
            q.setOptionA(options.get(0));
            q.setOptionB(options.get(1));
            q.setOptionC(options.get(2));
            q.setOptionD(options.get(3));
            q.setCorrectAnswer(correctKey);
            q.setCreatedBy(2L); // 정책에 맞게 수정 가능
            q.setCreatedAt(LocalDateTime.now());
            Quiz saved = quizRepo.save(q);

            // 2) 캐시에 정답 포함 문항 저장 (quizId 포함)
            questions.add(new QuizQuestion(saved.getQuizId(), prompt, options, answerIndex));
        }

        String quizUuid = UUID.randomUUID().toString();
        QuizPaper paper = new QuizPaper(quizUuid, category, mode, questions);
        quizCache.put(quizUuid, paper);

        // 사용자에겐 정답 제외된 뷰 제공
        List<QuizViewItem> items = paper.questions().stream()
                .map(q -> new QuizViewItem(q.prompt(), q.options()))
                .toList();

        return new QuizView(quizUuid, category, mode, items);
    }

    /**
     * 채점: 캐시된 QuizPaper 기준으로 채점(정답 비교), quiz_result에 저장, 문항별 상세 GradeResponse 반환
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Transactional
    public GradeResponse grade(String quizUuid, GradeRequest req, Long userId) {
        QuizPaper paper = quizCache.getIfPresent(quizUuid);
        if (paper == null)
            throw new ResponseStatusException(HttpStatus.GONE, "퀴즈가 만료되었거나 존재하지 않습니다.");

        int total = paper.questions().size();
        int correctCount = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            QuizQuestion q = paper.questions().get(i);
            Integer pick = req.answers().get(i); // 0..3 또는 null

            boolean isCorrect = (pick != null && pick >= 0 && pick < q.options().size() && q.answerIndex() == pick);
            if (isCorrect) correctCount++;

            String userKey = (pick == null || pick < 0 || pick > 3) ? null : indexToKey(pick);

            // DB 저장 (quiz_result)
            QuizResult r = new QuizResult();
            r.setUserId(userId);
            r.setQuizId(q.quizId());              // FK to quiz.quiz_id
            r.setUserAnswer(userKey);             // A/B/C/D or null
            r.setIsCorrect(isCorrect ? "Y" : "N");
            r.setCreatedAt(LocalDateTime.now());
            quizResultRepo.save(r);

            // 응답용 문항 결과 생성 (정답 인덱스 포함)
            results.add(new QuestionResult(
                    i,
                    q.prompt(),
                    q.options(),
                    pick,
                    q.answerIndex(),
                    isCorrect
            ));
        }

        // 필요하면 1회성 처리: quizCache.invalidate(quizUuid);

        return new GradeResponse(total, correctCount, results);
    }

    // ---- 내부 유틸 ----
    private List<String> buildOptions(String category, Word target, QuestionMode mode, String correctText) {
        List<String> options = new ArrayList<>(4);
        options.add(correctText);

        List<String> distractors;
        if (mode == QuestionMode.JP_TO_KR) {
            // 주의: Word 엔티티의 PK 접근자가 getId()가 아닐 경우(getWordId()) 여기 이름을 바꿔 주세요.
            distractors = wordRepo.findRandomMeaningsForDistractorsOracle(
                    category, target.getId(), PageRequest.of(0, 20));
        } else {
            distractors = wordRepo.findRandomJapaneseForDistractorsOracle(
                    category, target.getId(), PageRequest.of(0, 20));
        }

        distractors.removeIf(s -> s == null || s.isBlank() || s.equals(correctText));
        distractors = distractors.stream().distinct().toList();
        if (distractors.size() < 3)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "오답 후보가 부족합니다.");

        Collections.shuffle(distractors);
        options.addAll(distractors.subList(0, 3));
        Collections.shuffle(options);
        return options;
    }

    private String indexToKey(int idx) {
        return switch (idx) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            case 3 -> "D";
            default -> null;
        };
    }
}
