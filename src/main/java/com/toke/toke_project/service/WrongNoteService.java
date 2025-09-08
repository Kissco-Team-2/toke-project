package com.toke.toke_project.service;

import com.toke.toke_project.domain.Quiz;
import com.toke.toke_project.domain.Users;
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.domain.WrongNote;
import com.toke.toke_project.repo.QuizRepository;
import com.toke.toke_project.repo.QuizResultRepository;
import com.toke.toke_project.repo.WrongNoteRepository;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.web.dto.WrongNoteDto;
import com.toke.toke_project.web.dto.BulkDeleteResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WrongNote 관련 서비스
 */
@Service
public class WrongNoteService {

    private final WrongNoteRepository wrongNoteRepository;
    private final QuizRepository quizRepository;
    private final WordRepository wordRepository;
    private final QuizResultRepository quizResultRepository;

    @PersistenceContext
    private EntityManager em;

    public WrongNoteService(WrongNoteRepository wrongNoteRepository,
                            QuizRepository quizRepository,
                            WordRepository wordRepository,
                            QuizResultRepository quizResultRepository) {
        this.wrongNoteRepository = wrongNoteRepository;
        this.quizRepository = quizRepository;
        this.wordRepository = wordRepository;
        this.quizResultRepository = quizResultRepository;
    }

    /**
     * 안전한 등록: race condition 대비
     */
    @Transactional
    public WrongNote registerIfNotExists(Long userId, Long quizId) {
        // 1) 먼저 간단히 조회해서 있으면 반환
        Optional<WrongNote> exist = wrongNoteRepository.findByUser_IdAndQuiz_QuizId(userId, quizId);
        if (exist.isPresent()) return exist.get();

        // 2) 없는 경우 insert 시도 (EntityManager#getReference 로 proxy 참조)
        WrongNote wn = new WrongNote();
        Users userRef = em.getReference(Users.class, userId); // attach reference without select
        wn.setUser(userRef);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));
        wn.setQuiz(quiz);
        wn.setCreatedAt(LocalDateTime.now());
        wn.setStarred("N");

        try {
            return wrongNoteRepository.save(wn);
        } catch (DataIntegrityViolationException ex) {
            // concurrent insert -> 누군가 먼저 만들었을 경우 다시 조회해서 반환
            return wrongNoteRepository.findByUser_IdAndQuiz_QuizId(userId, quizId)
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * 단순 목록 (기본, 최신순)
     */
    @Transactional(readOnly = true)
    public List<WrongNoteDto> listByUser(Long userId) {
        List<WrongNote> notes = wrongNoteRepository.findByUserIdWithQuiz(userId);

        return notes.stream().map(wn -> toDto(wn, userId)).collect(Collectors.toList());
    }

    /**
     * 노트 내용 수정/생성
     */
    @Transactional
    public WrongNoteDto updateNote(Long noteId, String noteContent, String starred) {
        WrongNote wn = wrongNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("WrongNote not found: " + noteId));
        wn.setNote(noteContent);
        if (starred != null) wn.setStarred(starred);
        WrongNote saved = wrongNoteRepository.save(wn);

        return toDto(saved, saved.getUser() != null ? saved.getUser().getId() : null);
    }

    @Transactional
    public void deleteNote(Long noteId) {
        wrongNoteRepository.deleteById(noteId);
    }

    /**
     * starred 직접 설정 (Y/N)
     */
    @Transactional
    public WrongNoteDto setStarred(Long noteId, Long userId, boolean starredFlag) {
        String val = starredFlag ? "Y" : "N";

        int updated = wrongNoteRepository.updateStarredByNoteIdAndUserId(noteId, userId, val);
        if (updated == 0) {
            boolean exists = wrongNoteRepository.existsById(noteId);
            if (!exists) throw new IllegalArgumentException("WrongNote not found: " + noteId);
            else throw new SecurityException("Not allowed to change this note");
        }

        WrongNote wn = wrongNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("WrongNote not found after update: " + noteId));

        return toDto(wn, userId);
    }

    /**
     * 원자적 토글 (DB CASE문 사용)
     */
    @Transactional
    public WrongNoteDto toggleStar(Long noteId, Long userId) {
        WrongNote wn = wrongNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("WrongNote not found: " + noteId));

        if (!wn.getUser().getId().equals(userId)) {
            throw new SecurityException("Not allowed to toggle this note");
        }

        int updated = wrongNoteRepository.toggleStarByNoteIdAndUserId(noteId, userId);
        if (updated == 0) {
            throw new IllegalStateException("Toggle failed");
        }

        WrongNote updatedWn = wrongNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("WrongNote not found after toggle: " + noteId));
        return toDto(updatedWn, userId);
    }

    /**
     * 사용자별 starred 리스트 반환 (DTO)
     */
    @Transactional(readOnly = true)
    public List<WrongNoteDto> listStarredByUser(Long userId) {
        List<WrongNote> notes = wrongNoteRepository.findByUser_IdAndStarredOrderByCreatedAtDesc(userId, "Y");
        return notes.stream().map(wn -> toDto(wn, userId)).collect(Collectors.toList());
    }

    // --- 헬퍼: entity -> dto 매핑 ---
    private WrongNoteDto toDto(WrongNote wn, Long requestUserId) {
        WrongNoteDto dto = new WrongNoteDto();
        dto.setNoteId(wn.getNoteId());
        dto.setUserId(wn.getUser() != null ? wn.getUser().getId() : null);
        dto.setQuizId(wn.getQuiz() != null ? wn.getQuiz().getQuizId() : null);
        dto.setNote(wn.getNote());
        dto.setStarred(wn.getStarred());

        // wrong_note 생성일
        dto.setNoteCreatedAt(wn.getCreatedAt());

        Word w = (wn.getQuiz() != null) ? wn.getQuiz().getWord() : null;
        if (w == null && wn.getQuiz() != null && wn.getQuiz().getQuestion() != null) {
            w = wordRepository.findByJapaneseOrKorean(wn.getQuiz().getQuestion()).orElse(null);
        }
        if (w != null) {
            dto.setJapaneseWord(w.getJapaneseWord());
            dto.setReadingKana(w.getReadingKana());
            dto.setKoreanMeaning(w.getKoreanMeaning());
            dto.setExampleSentenceJp(w.getExampleSentenceJp());
            dto.setCategory(w.getCategory());
        } else {
            dto.setJapaneseWord(wn.getQuiz() != null ? wn.getQuiz().getQuestion() : null);
            dto.setCategory(null);
        }

        Long qId = dto.getQuizId();
        dto.setWrongCount(qId != null ? quizResultRepository.countWrongByUserAndQuiz(requestUserId, qId) : 0L);
        dto.setLastWrongAt(qId != null ? quizResultRepository.findLastWrongDateByUserAndQuiz(requestUserId, qId) : null);

        return dto;
    }

    /**
     * 필터/정렬/페이징된 오답노트 반환 (최신순/ 날짜별(1개월, 3개월, 지난달, 직접설정) / 카테고리별(고객대응, 메일, 보고, 인사, 전화, 제안, 회의))
     */
    @Transactional(readOnly = true)
    public Page<WrongNoteDto> listByUserWithFilters(
            Long userId,
            String sort,
            String dateFilter,
            String from,   // yyyy-MM-dd, optional (CUSTOM)
            String to,     // yyyy-MM-dd, optional (CUSTOM)
            String category,
            int page,
            int size) {

        // 1) 페치해서 DTO로 변환
        List<WrongNote> notes = wrongNoteRepository.findByUserIdWithQuiz(userId);
        List<WrongNoteDto> dtos = notes.stream()
                .map(wn -> toDto(wn, userId))
                .collect(Collectors.toList());

        // 2) date filter 범위 계산 (임시 변수에 할당 후 final로 복사)
        LocalDateTime tmpFrom = null;
        LocalDateTime tmpTo = null;
        if (dateFilter != null && !"ALL".equalsIgnoreCase(dateFilter)) {
            LocalDate now = LocalDate.now(ZoneId.systemDefault());
            switch (dateFilter) {
                case "1M":
                    tmpFrom = now.minusMonths(1).atStartOfDay();
                    tmpTo = now.plusDays(1).atStartOfDay();
                    break;
                case "3M":
                    tmpFrom = now.minusMonths(3).atStartOfDay();
                    tmpTo = now.plusDays(1).atStartOfDay();
                    break;
                case "LAST_MONTH":
                    LocalDate firstOfLast = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                    LocalDate lastOfLast  = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    tmpFrom = firstOfLast.atStartOfDay();
                    tmpTo   = lastOfLast.plusDays(1).atStartOfDay();
                    break;
                case "CUSTOM":
                    if (from != null) tmpFrom = LocalDate.parse(from).atStartOfDay();
                    if (to   != null) tmpTo   = LocalDate.parse(to).plusDays(1).atStartOfDay();
                    break;
                default:
                    break;
            }
        }

        final LocalDateTime filterFrom = tmpFrom;
        final LocalDateTime filterTo   = tmpTo;
        final String filterCategory = (category == null || category.isBlank()) ? null : category.trim();

        // 3) apply category & date filters (date 기준은 lastWrongAt)
        List<WrongNoteDto> filtered = dtos.stream()
                .filter(dto -> {
                    if (filterCategory != null) {
                        String dtoCat = dto.getCategory();
                        if (dtoCat == null) return false;
                        if (!dtoCat.equals(filterCategory)) return false;
                    }
                    if (filterFrom != null) {
                        LocalDateTime lastWrong = dto.getLastWrongAt();
                        if (lastWrong == null) return false;
                        if (lastWrong.isBefore(filterFrom)) return false;
                        if (filterTo != null && !lastWrong.isBefore(filterTo)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 4) 정렬
        Comparator<WrongNoteDto> comparator;
        if ("LAST_WRONG_DATE".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(WrongNoteDto::getLastWrongAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        } else { // default LATEST (wrong_note.created_at)
            comparator = Comparator.comparing(WrongNoteDto::getNoteCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        filtered.sort(comparator);

        // 5) 페이징
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<WrongNoteDto> pageContent = (fromIndex >= filtered.size()) ? List.of() : filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
    }
    
    @Transactional
    public BulkDeleteResponse deleteNotesBulk(List<Long> noteIds, Long userId) {
        // 1) 요청된 id들 중 DB에 존재하는 레코드 로드
        List<WrongNote> foundNotes = wrongNoteRepository.findAllById(noteIds);

        Set<Long> foundIds = foundNotes.stream().map(WrongNote::getNoteId).collect(Collectors.toSet());
        List<Long> notFound = noteIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        // 2) 소유자 검사: 요청자(userId)와 일치하는 것만 삭제 대상으로 선정
        List<WrongNote> ownedNotes = foundNotes.stream()
                .filter(wn -> wn.getUser() != null && wn.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        List<Long> ownedIds = ownedNotes.stream().map(WrongNote::getNoteId).collect(Collectors.toList());

        List<Long> notOwned = foundNotes.stream()
                .map(WrongNote::getNoteId)
                .filter(id -> !ownedIds.contains(id))
                .collect(Collectors.toList());

        // 3) 삭제 수행 (batch delete 시도)
        if (!ownedIds.isEmpty()) {
            try {
                // Spring Data JPA 2.5+ 에는 deleteAllByIdInBatch가 있음
                wrongNoteRepository.deleteAllByIdInBatch(ownedIds);
            } catch (NoSuchMethodError | UnsupportedOperationException ex) {
                // fallback: 하나씩 삭제 (트랜잭션 안에서 수행되므로 안전)
                ownedIds.forEach(wrongNoteRepository::deleteById);
            }
        }

        String message = String.format("%d deleted, %d not found, %d not owned",
                ownedIds.size(), notFound.size(), notOwned.size());

        return new BulkDeleteResponse(ownedIds, notFound, notOwned, message);
    }
    
}
