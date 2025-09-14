package com.toke.toke_project.service;


import com.toke.toke_project.domain.Users;
import com.toke.toke_project.domain.Word;
import com.toke.toke_project.domain.WrongNote;
import com.toke.toke_project.repo.WrongNoteRepository;
import com.toke.toke_project.repo.WordRepository;
import com.toke.toke_project.web.dto.WrongNoteDto;
import com.toke.toke_project.web.dto.BulkDeleteResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WrongNoteService {

    private final WrongNoteRepository wrongNoteRepository;
    private final WordRepository wordRepository;

    @PersistenceContext
    private EntityManager em;

    public WrongNoteService(WrongNoteRepository wrongNoteRepository,
                            WordRepository wordRepository) {
        this.wrongNoteRepository = wrongNoteRepository;
        this.wordRepository = wordRepository;
    }

    /* -------------------- 기본 유틸 / 목록 -------------------- */

    @Transactional(readOnly = true)
    public List<WrongNoteDto> listByUser(Long userId) {
        List<WrongNote> notes = wrongNoteRepository.findByUserIdWithWord(userId);
        return notes.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<WrongNoteDto> listByUserWithFilters(
            Long userId,
            String sort,
            String dateFilter,
            String from,
            String to,
            String category,
            int page,
            int size) {

        List<WrongNote> notes = wrongNoteRepository.findByUserIdWithWord(userId);
        List<WrongNoteDto> dtos = notes.stream().map(this::toDto).collect(Collectors.toList());

        LocalDateTime tmpFrom = null;
        LocalDateTime tmpTo = null;
        if (dateFilter != null && !"ALL".equalsIgnoreCase(dateFilter)) {
            LocalDate now = LocalDate.now(ZoneId.systemDefault());
            switch (dateFilter) {
                case "1M" -> { tmpFrom = now.minusMonths(1).atStartOfDay(); tmpTo = now.plusDays(1).atStartOfDay(); }
                case "3M" -> { tmpFrom = now.minusMonths(3).atStartOfDay(); tmpTo = now.plusDays(1).atStartOfDay(); }
                case "LAST_MONTH" -> {
                    LocalDate firstOfLast = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
                    LocalDate lastOfLast  = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    tmpFrom = firstOfLast.atStartOfDay();
                    tmpTo   = lastOfLast.plusDays(1).atStartOfDay();
                }
                case "CUSTOM" -> {
                    if (from != null) tmpFrom = LocalDate.parse(from).atStartOfDay();
                    if (to   != null) tmpTo   = LocalDate.parse(to).plusDays(1).atStartOfDay();
                }
                default -> {}
            }
        }

        final LocalDateTime filterFrom = tmpFrom;
        final LocalDateTime filterTo   = tmpTo;
        final String filterCategory = (category == null || category.isBlank()) ? null : category.trim();

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

        Comparator<WrongNoteDto> comparator;
        if ("LAST_WRONG_DATE".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(WrongNoteDto::getLastWrongAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            comparator = Comparator.comparing(WrongNoteDto::getNoteCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
        filtered.sort(comparator);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<WrongNoteDto> pageContent = (fromIndex >= filtered.size()) ? List.of() : filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, PageRequest.of(page, size), filtered.size());
    }

    /* -------------------- 오답 기록 (word 기준) -------------------- */

    @Transactional
    public void recordWrong(Long userId, Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));

        WrongNote wn = wrongNoteRepository.findByUser_IdAndWord_Id(userId, word.getId())
                .orElseGet(() -> {
                    WrongNote n = new WrongNote();
                    n.setUser(em.getReference(Users.class, userId));
                    n.setWord(word);
                    n.setCreatedAt(LocalDateTime.now());
                    n.setStarred("N");
                    n.setWrongCount(0L);
                    return n;
                });

        wn.setWrongCount( (wn.getWrongCount() == null ? 0L : wn.getWrongCount()) + 1 );
        wn.setLastWrongAt(LocalDateTime.now());

        wrongNoteRepository.save(wn);
    }

    /* -------------------- CRUD: 수정/삭제/토글/별표 리스트 -------------------- */

    @Transactional
    public WrongNoteDto updateNote(Long noteId, Long userId, String noteContent, String starred) {
        WrongNote wn = wrongNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("WrongNote not found: " + noteId));

        if (wn.getUser() == null || !wn.getUser().getId().equals(userId)) {
            throw new SecurityException("Not allowed to edit this note");
        }

        wn.setNote(noteContent);
        if (starred != null) wn.setStarred(starred);

        WrongNote saved = wrongNoteRepository.save(wn);
        return toDto(saved);
    }

    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        WrongNote wn = wrongNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("WrongNote not found: " + noteId));
        if (wn.getUser() == null || !wn.getUser().getId().equals(userId)) {
            throw new SecurityException("Not allowed to delete this note");
        }
        wrongNoteRepository.delete(wn);
    }

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

        return toDto(wn);
    }

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
        return toDto(updatedWn);
    }

    @Transactional(readOnly = true)
    public List<WrongNoteDto> listStarredByUser(Long userId) {
        List<WrongNote> notes = wrongNoteRepository.findByUser_IdAndStarredOrderByCreatedAtDesc(userId, "Y");
        return notes.stream().map(this::toDto).collect(Collectors.toList());
    }

    /* -------------------- 일괄 삭제 -------------------- */

    @Transactional
    public BulkDeleteResponse deleteNotesBulk(List<Long> noteIds, Long userId) {
        List<WrongNote> foundNotes = wrongNoteRepository.findAllById(noteIds);

        Set<Long> foundIds = foundNotes.stream().map(WrongNote::getNoteId).collect(Collectors.toSet());
        List<Long> notFound = noteIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());

        List<WrongNote> ownedNotes = foundNotes.stream()
                .filter(wn -> wn.getUser() != null && wn.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        List<Long> ownedIds = ownedNotes.stream().map(WrongNote::getNoteId).collect(Collectors.toList());

        List<Long> notOwned = foundNotes.stream()
                .map(WrongNote::getNoteId)
                .filter(id -> !ownedIds.contains(id))
                .collect(Collectors.toList());

        if (!ownedIds.isEmpty()) {
            try {
                wrongNoteRepository.deleteAllByIdInBatch(ownedIds);
            } catch (NoSuchMethodError | UnsupportedOperationException ex) {
                ownedIds.forEach(wrongNoteRepository::deleteById);
            }
        }

        String message = String.format("%d deleted, %d not found, %d not owned",
                ownedIds.size(), notFound.size(), notOwned.size());

        return new BulkDeleteResponse(ownedIds, notFound, notOwned, message);
    }

    /* -------------------- helper: entity -> dto -------------------- */

    private WrongNoteDto toDto(WrongNote wn) {
        WrongNoteDto dto = new WrongNoteDto();
        dto.setNoteId(wn.getNoteId());
        dto.setUserId(wn.getUser() != null ? wn.getUser().getId() : null);
        dto.setWordId(wn.getWord() != null ? wn.getWord().getId() : null);
        dto.setNote(wn.getNote());
        dto.setStarred(wn.getStarred());
        dto.setNoteCreatedAt(wn.getCreatedAt());
        dto.setWrongCount(wn.getWrongCount());
        dto.setLastWrongAt(wn.getLastWrongAt());

        Word w = wn.getWord();
        if (w != null) {
            dto.setJapaneseWord(w.getJapaneseWord());
            dto.setReadingKana(w.getReadingKana());
            dto.setKoreanMeaning(w.getKoreanMeaning());
            dto.setExampleSentenceJp(w.getExampleSentenceJp());
            dto.setCategory(w.getCategory());
       
        }

        return dto;
    }
}
