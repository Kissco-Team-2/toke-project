package com.toke.toke_project.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.toke.toke_project.domain.QuizResult;
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.domain.WrongNote;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.repo.WrongNoteRepository;
import com.toke.toke_project.service.model.QuestionMode;
import com.toke.toke_project.service.model.QuizPaper;
import com.toke.toke_project.service.model.QuizQuestion;
import com.toke.toke_project.web.dto.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final WordRepository wordRepository;
    private final WrongNoteRepository wrongNoteRepository;
    private final WrongNoteService wrongNoteService;
    private final QuizResultService quizResultService;

    private final Cache<String, QuizPaper> quizCache;

    @PersistenceContext
    private EntityManager em;

    public QuizService(WordRepository wordRepository,
                       WrongNoteRepository wrongNoteRepository,
                       WrongNoteService wrongNoteService,
                       QuizResultService quizResultService,
                       Cache<String, QuizPaper> quizCache) {
        this.wordRepository = wordRepository;
        this.wrongNoteRepository = wrongNoteRepository;
        this.wrongNoteService = wrongNoteService;
        this.quizResultService = quizResultService;
        this.quizCache = quizCache;
    }

    /** 오답노트 기반 퀴즈 생성 */
    @Transactional
    public QuizView generateFromWrongNotesForUser(Long userId, WrongNoteQuizRequest req) {
        List<WrongNote> notes = wrongNoteRepository.findByUserIdWithWord(userId);

        // 날짜 필터
        LocalDateTime fFrom = null, fTo = null;
        if (req.getDateFrom() != null) fFrom = LocalDate.parse(req.getDateFrom()).atStartOfDay();
        if (req.getDateTo()   != null) fTo   = LocalDate.parse(req.getDateTo()).plusDays(1).atStartOfDay();
        final LocalDateTime filterFrom = fFrom;
        final LocalDateTime filterTo   = fTo;
        final String categoryFilter = (req.getCategory() == null || req.getCategory().isBlank())
                ? null : req.getCategory().trim();

        // 후보 단어 필터링
        List<Word> candidates = notes.stream()
                .filter(wn -> {
                    if (categoryFilter != null) {
                        String cat = wn.getWord().getCategory();
                        if (cat == null || !cat.equals(categoryFilter)) return false;
                    }
                    if (filterFrom != null) {
                        LocalDateTime lastWrong = wn.getLastWrongAt();
                        if (lastWrong == null || lastWrong.isBefore(filterFrom)) return false;
                        if (filterTo != null && !lastWrong.isBefore(filterTo)) return false;
                    }
                    return true;
                })
                .map(WrongNote::getWord)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("조건에 맞는 오답노트가 없습니다.");
        }

        int n = (req.getCount() == null || req.getCount() <= 0)
                ? Math.min(10, candidates.size())
                : Math.min(req.getCount(), candidates.size());
        Collections.shuffle(candidates);
        List<Word> picked = candidates.subList(0, n);

        QuestionMode mode = QuestionMode.JP_TO_KR;

        // QuizPaper + ViewItem 동시에 구성
        List<QuizQuestion> questions = new ArrayList<>();
        List<QuizViewItem> items = new ArrayList<>();

        for (Word w : picked) {
            String prompt, correctText;
            if (mode == QuestionMode.JP_TO_KR) {
                prompt = "'" + w.getJapaneseWord() + "'란 무슨 뜻입니까?";
                correctText = w.getKoreanMeaning();
            } else {
                prompt = "'" + w.getKoreanMeaning() + "'에 해당하는 일본어는?";
                correctText = w.getJapaneseWord();
            }

            List<String> options = buildOptions(w.getCategory(), w, mode, correctText);
            int answerIndex = options.indexOf(correctText);

            // ✅ 오타 수정: geId → getId
            QuizQuestion q = new QuizQuestion(
                    w.getId(),   // quizId 자리엔 wordId를 넣어 둠
                    prompt,
                    options,
                    answerIndex
            );
            questions.add(q);
            items.add(new QuizViewItem(prompt, options));
        }

        String quizUuid = UUID.randomUUID().toString();
        QuizPaper paper = new QuizPaper(
                quizUuid,
                categoryFilter == null ? "오답노트" : categoryFilter,
                mode,
                questions
        );

        quizCache.put(quizUuid, paper);

        return new QuizView(
                quizUuid,
                categoryFilter == null ? "오답노트" : categoryFilter,
                mode,
                items
        );
    }

    /** 채점 */
    @PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
    @Transactional
    public GradeResponse grade(String quizUuid, GradeRequest req, Long userId) {
        QuizPaper paper = quizCache.getIfPresent(quizUuid);
        if (paper == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "퀴즈가 만료되었거나 존재하지 않습니다.");
        }

        int total = paper.questions().size();
        Map<Integer, Integer> answerMap = (req != null && req.answers() != null)
                ? req.answers() : Collections.emptyMap();

        int correctCount = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            var q = paper.questions().get(i);
            Integer pick = answerMap.get(i);

            boolean isCorrect = (pick != null && pick >= 0 && pick < q.options().size() && q.answerIndex() == pick);
            if (isCorrect) correctCount++;

            // 풀이 기록 저장 (wordId를 QuizResult.quizId 필드에 저장)
            QuizResult r = new QuizResult();
            r.setUserId(userId);
            r.setWordId(q.quizId()); // wordId 저장
            r.setUserAnswer((pick == null || pick < 0 || pick > 3) ? null : indexToKey(pick));
            r.setIsCorrect(isCorrect ? "Y" : "N");
            r.setCreatedAt(LocalDateTime.now());
            quizResultService.saveResult(r);

       

            // 해설/예문
            Word w = wordRepository.findById((long) q.quizId()).orElse(null);
            String explain;
            String ex;
            if (w != null) {
                if (paper.mode() == QuestionMode.JP_TO_KR) {
                    explain = String.format("'%s'(%s)의 뜻은 '%s' 입니다.",
                            ns(w.getJapaneseWord()), ns(w.getReadingKana()), ns(w.getKoreanMeaning()));
                } else {
                    explain = String.format("'%s'에 해당하는 일본어는 '%s'(%s) 입니다.",
                            ns(w.getKoreanMeaning()), ns(w.getJapaneseWord()), ns(w.getReadingKana()));
                }
                ex = w.getExampleSentenceJp();
            } else {
                explain = "해설: 정답을 중심으로 의미 차이를 확인해 보세요.";
                ex = null;
            }

            // ✅ 누락된 메서드 구현
            List<String> optionExplanations = buildOptionExplanations(paper.mode(), q.options(), q.answerIndex());

            results.add(new QuestionResult(
                    i,
                    q.prompt(),
                    q.options(),
                    pick,
                    q.answerIndex(),
                    isCorrect,
                    explain,
                    ex,
                    optionExplanations
            ));
        }

        return new GradeResponse(total, correctCount, results);
    }

    /** 보기 4개 구성 */
    private List<String> buildOptions(String category, Word target, QuestionMode mode, String correctText) {
        List<String> options = new ArrayList<>();
        options.add(correctText);

        List<String> distractors = (mode == QuestionMode.JP_TO_KR)
                ? wordRepository.findRandomMeaningsForDistractorsOracle(category, target.getId(), 20)
                : wordRepository.findRandomJapaneseForDistractorsOracle(category, target.getId(), 20);

        distractors = distractors.stream()
                .filter(s -> s != null && !s.isBlank() && !s.equals(correctText))
                .distinct()
                .collect(Collectors.toList());

        Collections.shuffle(distractors);
        options.addAll(distractors.subList(0, Math.min(3, distractors.size())));
        Collections.shuffle(options);
        return options;
    }

    /** ✅ 옵션별 간단 해설 (필요시 고도화 가능) */
    private List<String> buildOptionExplanations(QuestionMode mode, List<String> options, int answerIndex) {
        List<String> list = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            list.add(i == answerIndex ? ("정답: " + opt) : opt);
        }
        return list;
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

    private static String ns(String s) { return s == null ? "" : s; }
    
    /** -------------------- 기존 생성 메서드들 (관리자 / 일반) -------------------- */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public QuizView generate(GenerateRequest req) {
        return generateInternal(req);
    }

    @Transactional
    public QuizView generateForUser(GenerateRequest req) {
        return generateInternal(req);
    }

    private QuizView generateInternal(GenerateRequest req) {
        String rawCategory = req.category();
        String category = (rawCategory == null || rawCategory.isBlank()) ? null : rawCategory.trim();
        boolean isAll = (category == null || "전체".equals(category));

        QuestionMode mode = (req.mode() == null) ? QuestionMode.JP_TO_KR : req.mode();
        int n = (req.questionCount() == null || req.questionCount() <= 0) ? 10 : req.questionCount();

        List<Word> picked = isAll
                ? wordRepository.findRandomOracle(n)
                : wordRepository.findRandomByCategoryOracleFlex(category, n);

        if (picked.size() < n) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 카테고리에 문제가 부족합니다.");
        }

        List<QuizQuestion> questions = new ArrayList<>();
        List<QuizViewItem> items = new ArrayList<>();

        for (Word w : picked) {
            String prompt, correctText;
            if (mode == QuestionMode.JP_TO_KR) {
                prompt = "'" + w.getJapaneseWord() + "'란 무슨 뜻입니까?";
                correctText = w.getKoreanMeaning();
            } else {
                prompt = "'" + w.getKoreanMeaning() + "'에 해당하는 일본어는?";
                correctText = w.getJapaneseWord();
            }

            List<String> options = buildOptions(w.getCategory(), w, mode, correctText);
            int answerIndex = options.indexOf(correctText);

            QuizQuestion q = new QuizQuestion(w.getId(), prompt, options, answerIndex);
            questions.add(q);
            items.add(new QuizViewItem(prompt, options));
        }

        String quizUuid = UUID.randomUUID().toString();
        QuizPaper paper = new QuizPaper(quizUuid, isAll ? "전체" : category, mode, questions);

        quizCache.put(quizUuid, paper);

        return new QuizView(
                quizUuid,
                isAll ? "전체" : category,
                mode,
                items
        );
    }

    /** -------------------- 캐시된 퀴즈 조회 -------------------- */
    @Transactional(readOnly = true)
    public QuizView getViewByUuid(String uuid) {
        QuizPaper paper = quizCache.getIfPresent(uuid);
        if (paper == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "퀴즈가 만료되었습니다.");
        }
        var items = paper.questions().stream()
                .map(q -> new QuizViewItem(q.prompt(), q.options()))
                .toList();
        return new QuizView(uuid, paper.category(), paper.mode(), items);
    }

}
