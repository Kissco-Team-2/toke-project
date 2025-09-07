package com.toke.toke_project.service;

import java.util.stream.Collectors;
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

    /* -------------------- 생성 (관리자) -------------------- */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public QuizView generate(GenerateRequest req) {
        return generateInternal(req);
    }

    /* -------------------- 생성 (사용자) -------------------- */
    @Transactional
    public QuizView generateForUser(GenerateRequest req) {
        return generateInternal(req);
    }

    /* 공통 내부 구현 */
    private QuizView generateInternal(GenerateRequest req) {
        String rawCategory = req.category();
        String category = normalizeCategory(rawCategory);              // "고객 대응" -> "고객대응" 등
        boolean isAll = isAllCategory(category);

        QuestionMode mode = (req.mode() == null) ? QuestionMode.JP_TO_KR : req.mode();
        int n = (req.questionCount() == null || req.questionCount() <= 0) ? 10 : req.questionCount();

     // 1) 문제용 단어 뽑기
        List<Word> picked = isAll
                ? wordRepo.findRandomOracle(n)
                : wordRepo.findRandomByCategoryOracleFlex(category, n);

        if (picked.size() < n) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 카테고리에 문제가 부족합니다.");
        }

        List<QuizQuestion> questions = new ArrayList<>();

        // 2) 각 문항 구성 + quiz 저장
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

            // “전체”를 택한 경우 보기 후보는 해당 단어의 실제 카테고리에서 뽑아서 맥락 유지
            String distractorCategory = isAll ? w.getCategory() : category;

            List<String> options = buildOptions(distractorCategory, w, mode, correctText);
            int answerIndex = options.indexOf(correctText);
            String correctKey = indexToKey(answerIndex);

            Quiz q = new Quiz();
            q.setQuestionType(mode.name());
            q.setQuestion(prompt);
            q.setOptionA(options.get(0));
            q.setOptionB(options.get(1));
            q.setOptionC(options.get(2));
            q.setOptionD(options.get(3));
            q.setCorrectAnswer(correctKey);
            q.setCreatedBy(2L); // 필요 시 실제 사용자/관리자 id로 교체
            q.setCreatedAt(LocalDateTime.now());

            Quiz saved = quizRepo.save(q);
            questions.add(new QuizQuestion(saved.getQuizId(), prompt, options, answerIndex));
        }

        // 3) 캐시에 저장 & 사용자에게 뷰 반환(정답 제외)
        String quizUuid = UUID.randomUUID().toString();
        QuizPaper paper = new QuizPaper(quizUuid, isAll ? "전체" : category, mode, questions);
        quizCache.put(quizUuid, paper);

        List<QuizViewItem> items = paper.questions().stream()
                .map(q -> new QuizViewItem(q.prompt(), q.options()))
                .toList();

        return new QuizView(quizUuid, isAll ? "전체" : category, mode, items);
    }

    /* -------------------- 채점 -------------------- */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Transactional
    public GradeResponse grade(String quizUuid, GradeRequest req, Long userId) {
        QuizPaper paper = quizCache.getIfPresent(quizUuid);
        if (paper == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "퀴즈가 만료되었거나 존재하지 않습니다.");
        }

        int total = paper.questions().size();

        // answers: Map<문항index, 선택지index>
        Map<Integer, Integer> answerMap =
                (req != null && req.answers() != null) ? req.answers() : Collections.emptyMap();

        int correctCount = 0;                          // ✅ 누락되었던 선언
        List<QuestionResult> results = new ArrayList<>(); // ✅ 누락되었던 선언

        for (int i = 0; i < total; i++) {
            QuizQuestion q = paper.questions().get(i);

            // 사용자가 고른 답(없으면 null)
            Integer pick = answerMap.get(i);

            boolean isCorrect =
                    (pick != null && pick >= 0 && pick < q.options().size() && q.answerIndex() == pick);
            if (isCorrect) correctCount++;

            String userKey = (pick == null || pick < 0 || pick > 3) ? null : indexToKey(pick);

            // 결과 저장 (quiz_result)
            QuizResult r = new QuizResult();
            r.setUserId(userId);
            r.setQuizId(q.quizId());
            r.setUserAnswer(userKey);             // "A"/"B"/"C"/"D" or null
            r.setIsCorrect(isCorrect ? "Y" : "N");
            r.setCreatedAt(LocalDateTime.now());
            quizResultRepo.save(r);

            // 프론트에 돌려줄 문항별 결과
            results.add(new QuestionResult(
                    i,
                    q.prompt(),
                    q.options(),
                    pick,               // Integer 그대로(null 허용)
                    q.answerIndex(),
                    isCorrect
            ));
        }

        // 필요 시 1회성 퀴즈로 만들려면 주석 해제
        // quizCache.invalidate(quizUuid);

        return new GradeResponse(total, correctCount, results);
    }

    /* -------------------- 내부 유틸 -------------------- */
    private List<String> buildOptions(String category, Word target, QuestionMode mode, String correctText) {
        List<String> options = new ArrayList<>(4);
        options.add(correctText);

     // 보기(오답) 뽑기
        List<String> distractors = (mode == QuestionMode.JP_TO_KR)
                ? wordRepo.findRandomMeaningsForDistractorsOracle(category, target.getId(), 20)
                : wordRepo.findRandomJapaneseForDistractorsOracle(category, target.getId(), 20);

        // null/빈문자/정답 제거 + 중복 제거 후 "수정 가능한" 리스트로 변환
        distractors = distractors.stream()
                .filter(s -> s != null && !s.isBlank() && !s.equals(correctText))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        if (distractors.size() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "오답 후보가 부족합니다.");
        }

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

    private String normalizeCategory(String in) {
        if (in == null) return null;
        String s = in.trim();
        // 공백/특수기호 제거해서 저장된 카테고리와 맞추고 싶다면 아래 라인도 가능:
        // s = s.replaceAll("\\s+", "");
        if ("고객 대응".equals(s)) s = "고객대응"; // 버튼 라벨 보정
        return s;
    }
    private boolean isAllCategory(String s) {
        return s == null || s.isBlank() || "전체".equals(s);
    }
}
