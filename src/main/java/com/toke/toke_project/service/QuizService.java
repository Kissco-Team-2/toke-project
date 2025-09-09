package com.toke.toke_project.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.toke.toke_project.domain.Quiz;
import com.toke.toke_project.domain.QuizResult;
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.domain.WrongNote;
import com.toke.toke_project.repo.QuizRepository;
import com.toke.toke_project.repo.QuizResultRepository;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.repo.WrongNoteRepository;
import com.toke.toke_project.service.model.*;
import com.toke.toke_project.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quiz 관련 서비스: 기존 생성(관리자), 채점(사용자) 외에
 * - 오답노트 기반 퀴즈 생성(generateFromWrongNotesForUser) 추가
 */
@Service
public class QuizService {

    private final WordRepository wordRepo;
    private final QuizRepository quizRepo;
    private final QuizResultRepository quizResultRepo;
    private final WrongNoteRepository wrongNoteRepo;
    private final com.toke.toke_project.service.QuizResultService quizResultService;
    private final Cache<String, QuizPaper> quizCache;

    public QuizService(WordRepository wordRepo,
                       QuizRepository quizRepo,
                       QuizResultRepository quizResultRepo,
                       WrongNoteRepository wrongNoteRepo,
                       com.toke.toke_project.service.QuizResultService quizResultService,
                       Cache<String, QuizPaper> quizCache) {
        this.wordRepo = wordRepo;
        this.quizRepo = quizRepo;
        this.quizResultRepo = quizResultRepo;
        this.wrongNoteRepo = wrongNoteRepo;
        this.quizResultService = quizResultService;
        this.quizCache = quizCache;
    }

    /* -------------------- 기존 생성 메서드들 (관리자 / 일반) -------------------- */
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
        String category = normalizeCategory(rawCategory);
        boolean isAll = isAllCategory(category);

        QuestionMode mode = (req.mode() == null) ? QuestionMode.JP_TO_KR : req.mode();
        int n = (req.questionCount() == null || req.questionCount() <= 0) ? 10 : req.questionCount();

        List<Word> picked = isAll
                ? wordRepo.findRandomOracle(n)
                : wordRepo.findRandomByCategoryOracleFlex(category, n);

        if (picked.size() < n) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 카테고리에 문제가 부족합니다.");
        }

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
            q.setCreatedBy(2L); // 관리자 or system
            q.setCreatedAt(LocalDateTime.now());
            q.setWord(w);

            Quiz saved = quizRepo.save(q);
            questions.add(new QuizQuestion(saved.getQuizId(), prompt, options, answerIndex));
        }

        String quizUuid = UUID.randomUUID().toString();
        QuizPaper paper = new QuizPaper(quizUuid, isAll ? "전체" : category, mode, questions);
        quizCache.put(quizUuid, paper);

        List<QuizViewItem> items = paper.questions().stream()
                .map(q -> new QuizViewItem(q.prompt(), q.options()))
                .toList();

        return new QuizView(quizUuid, isAll ? "전체" : category, mode, items);
    }

    /* -------------------- 오답노트 기반 퀴즈 생성 -------------------- */
    @Transactional
    public QuizView generateFromWrongNotesForUser(Long userId, WrongNoteQuizRequest req) {
        // 1) fetch user's wrong notes with quiz+word eagerly
        List<WrongNote> notes = wrongNoteRepo.findByUserIdWithQuiz(userId);

        // 2) apply filters (date range and/or category)
        LocalDateTime from = null, to = null;
        if (req.getDateFrom() != null) {
            try { from = LocalDate.parse(req.getDateFrom()).atStartOfDay(); } catch (DateTimeParseException e) { /* ignore -> null */ }
        }
        if (req.getDateTo() != null) {
            try { to = LocalDate.parse(req.getDateTo()).plusDays(1).atStartOfDay(); } catch (DateTimeParseException e) { /* ignore */ }
        }
        final LocalDateTime fFrom = from;
        final LocalDateTime fTo   = to;
        final String catFilter = (req.getCategory() == null || req.getCategory().isBlank()) ? null : req.getCategory().trim();

        List<Word> candidates = notes.stream()
                .map(wn -> {
                    // prefer associated Word via wn.quiz.word, otherwise try to lookup by quiz.question
                    Word w = (wn.getQuiz() != null) ? wn.getQuiz().getWord() : null;
                    if (w == null && wn.getQuiz() != null && wn.getQuiz().getQuestion() != null) {
                        w = wordRepo.findByJapaneseOrKorean(wn.getQuiz().getQuestion()).orElse(null);
                    }
                    return new AbstractMap.SimpleEntry<WrongNote, Word>(wn, w);
                })
                .filter(e -> e.getValue() != null) // must have Word to generate quiz
                .filter(e -> {
                    Word w = e.getValue();
                    WrongNote wn = e.getKey();
                    if (catFilter != null) {
                        if (w.getCategory() == null) return false;
                        if (!w.getCategory().equals(catFilter)) return false;
                    }
                    if (fFrom != null || fTo != null) {
                        LocalDateTime lastWrong = quizResultRepo.findLastWrongDateByUserAndQuiz(userId, wn.getQuiz().getQuizId());
                        if (lastWrong == null) return false;
                        if (fFrom != null && lastWrong.isBefore(fFrom)) return false;
                        if (fTo != null && !lastWrong.isBefore(fTo)) return false;
                    }
                    return true;
                })
                .map(AbstractMap.SimpleEntry::getValue)
                .distinct()
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 조건에 맞는 오답이 없습니다.");
        }

        int n = (req.getCount() == null || req.getCount() <= 0) ? Math.min(10, candidates.size()) : Math.min(req.getCount(), candidates.size());
        Collections.shuffle(candidates);
        List<Word> picked = candidates.subList(0, n);

        // Build quizzes from picked words, reuse buildOptions etc.
        QuestionMode mode = QuestionMode.JP_TO_KR; // or allow request to specify; default JP_TO_KR
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

            String distractorCategory = (catFilter == null) ? w.getCategory() : catFilter;
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
            q.setCreatedBy(userId); // created by the user (or system id if you prefer)
            q.setCreatedAt(LocalDateTime.now());
            q.setWord(w); // link to Word - ensure Quiz.word mapping exists

            Quiz saved = quizRepo.save(q);
            questions.add(new QuizQuestion(saved.getQuizId(), prompt, options, answerIndex));
        }

        String quizUuid = UUID.randomUUID().toString();
        QuizPaper paper = new QuizPaper(quizUuid, catFilter == null ? "오답노트" : catFilter, mode, questions);
        quizCache.put(quizUuid, paper);

        List<QuizViewItem> items = paper.questions().stream()
                .map(q -> new QuizViewItem(q.prompt(), q.options()))
                .toList();

        return new QuizView(quizUuid, catFilter == null ? "오답노트" : catFilter, mode, items);
    }

    /* -------------------- 채점 (grade) -------------------- */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Transactional
    public GradeResponse grade(String quizUuid, GradeRequest req, Long userId) {
        QuizPaper paper = quizCache.getIfPresent(quizUuid);
        if (paper == null) throw new ResponseStatusException(HttpStatus.GONE, "퀴즈가 만료되었거나 존재하지 않습니다.");

        int total = paper.questions().size();
        Map<Integer, Integer> answerMap = (req != null && req.answers() != null) ? req.answers() : Collections.emptyMap();

        int correctCount = 0;
        List<QuestionResult> results = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            QuizQuestion q = paper.questions().get(i);
            Integer pick = answerMap.get(i);

            boolean isCorrect = (pick != null && pick >= 0 && pick < q.options().size() && q.answerIndex() == pick);
            if (isCorrect) correctCount++;

            // 저장은 반드시 서비스로! (오답이면 여기서 WrongNote 등록됨)
            QuizResult r = new QuizResult();
            r.setUserId(userId);
            r.setQuizId(q.quizId());
            r.setUserAnswer((pick == null || pick < 0 || pick > 3) ? null : indexToKey(pick));
            r.setIsCorrect(isCorrect ? "Y" : "N");
            r.setCreatedAt(LocalDateTime.now());
            quizResultService.saveResult(r); // ★ 저장

            // ---- 정답 해설/예문 채우기 ----
            String explain;
            String ex;

            // Quiz -> Word 추적
            Word w = null;
            Optional<Quiz> oq = quizRepo.findById(q.quizId());
            if (oq.isPresent()) w = oq.get().getWord();

            final String correctText = q.options().get(q.answerIndex());
            if (w == null) {
            	w = wordRepo.findAnyByJapaneseOrKorean(correctText).orElse(null);
            }

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

            // ---- ★ 각 선지별 해설 생성 ----
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
                    optionExplanations // ★ 추가
            ));
        }

        return new GradeResponse(total, correctCount, results);
    }

    /* ===== 각 보기별 해설 생성 ===== */
    private List<String> buildOptionExplanations(QuestionMode mode, List<String> options, int correctIndex) {
        List<String> list = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            String msg;

            // DB에서 해당 보기 텍스트로 Word 추적 시도
            Word w = wordRepo.findAnyByJapaneseOrKorean(opt).orElse(null);

            if (mode == QuestionMode.JP_TO_KR) {
                // 보기 = 한국어 뜻
                if (i == correctIndex) {
                    if (w != null) {
                        msg = String.format("정답: '%s'(%s)의 뜻이 바로 '%s'입니다.",
                                ns(w.getJapaneseWord()), ns(w.getReadingKana()), ns(w.getKoreanMeaning()));
                    } else {
                        msg = "정답 의미입니다.";
                    }
                } else {
                    if (w != null) {
                        msg = String.format("'%s'(%s)의 뜻이 '%s'이므로, 이번 문제의 정답 의미와는 다릅니다.",
                                ns(w.getJapaneseWord()), ns(w.getReadingKana()), ns(w.getKoreanMeaning()));
                    } else {
                        msg = "의미가 유사해 보일 수 있지만, 문맥과 정확한 대응이 다릅니다.";
                    }
                }
            } else { // KR_TO_JP
                // 보기 = 일본어 표기
                if (i == correctIndex) {
                    if (w != null) {
                        msg = String.format("정답: 한국어 '%s'에 해당하는 일본어가 '%s'(%s)입니다.",
                                ns(w.getKoreanMeaning()), ns(w.getJapaneseWord()), ns(w.getReadingKana()));
                    } else {
                        msg = "정답 일본어 표기입니다.";
                    }
                } else {
                    if (w != null) {
                        msg = String.format("'%s'(%s)는 '%s'의 의미로, 이번 문제의 한국어 뜻과는 일치하지 않습니다.",
                                ns(w.getJapaneseWord()), ns(w.getReadingKana()), ns(w.getKoreanMeaning()));
                    } else {
                        msg = "형태가 비슷하지만 의미가 다릅니다.";
                    }
                }
            }

            list.add(msg);
        }
        return list;
    }

    private static String ns(String s){ return s==null? "" : s; }

    /* -------------------- 내부 유틸: buildOptions, indexToKey, normalizeCategory... -------------------- */
    private List<String> buildOptions(String category, Word target, QuestionMode mode, String correctText) {
        List<String> options = new ArrayList<>(4);
        options.add(correctText);

        List<String> distractors = (mode == QuestionMode.JP_TO_KR)
                ? wordRepo.findRandomMeaningsForDistractorsOracle(category, target.getId(), 20)
                : wordRepo.findRandomJapaneseForDistractorsOracle(category, target.getId(), 20);

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
        if ("고객 대응".equals(s)) s = "고객대응";
        return s;
    }
    private boolean isAllCategory(String s) {
        return s == null || s.isBlank() || "전체".equals(s);
    }

    @Transactional(readOnly = true)
    public QuizView getViewByUuid(String uuid) {
        var paper = quizCache.getIfPresent(uuid);
        if (paper == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.GONE, "퀴즈가 만료되었습니다.");
        var items = paper.questions().stream()
                .map(q -> new com.toke.toke_project.web.dto.QuizViewItem(q.prompt(), q.options()))
                .toList();
        return new com.toke.toke_project.web.dto.QuizView(uuid, paper.category(), paper.mode(), items);
    }

}
