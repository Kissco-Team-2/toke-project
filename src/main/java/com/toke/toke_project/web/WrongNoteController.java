package com.toke.toke_project.web;

import com.toke.toke_project.security.CustomUserDetails;
import com.toke.toke_project.service.WrongNoteService;
import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.web.dto.*;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@RestController
@RequestMapping("/api/wrong-notes")
public class WrongNoteController {

    private final WrongNoteService wrongNoteService;
    private final QuizService quizService;

    public WrongNoteController(WrongNoteService wrongNoteService,
                               QuizService quizService) {
        this.wrongNoteService = wrongNoteService;
        this.quizService = quizService;
    }

    /** 오답 목록 (기본 최신순) */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WrongNoteDto>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();
        return ResponseEntity.ok(wrongNoteService.listByUser(userId));
    }

    /** 오답 목록(필터 포함, 페이지네이션) */
    @GetMapping("/filter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WrongNoteDto>> listFiltered(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(value = "sort",       defaultValue = "LATEST") String sort,
            @RequestParam(value = "dateFilter", defaultValue = "ALL")    String dateFilter,
            @RequestParam(value = "from",       required = false)        String from,
            @RequestParam(value = "to",         required = false)        String to,
            @RequestParam(value = "category",   required = false)        String category,
            @RequestParam(value = "page",       defaultValue = "0")      int page,
            @RequestParam(value = "size",       defaultValue = "20")     int size) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        Page<WrongNoteDto> res = wrongNoteService
                .listByUserWithFilters(userId, sort, dateFilter, from, to, category, page, size);
        return ResponseEntity.ok(res);
    }

    /** 메모 저장/수정 */
    @PatchMapping("/{noteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WrongNoteDto> updateNote(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long noteId,
            @RequestBody UpdateNoteRequest req) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        WrongNoteDto dto = wrongNoteService.updateNote(noteId, userId, req.getNote(), req.getStarred());
        return ResponseEntity.ok(dto);
    }

    /** 단건 삭제 */
    @DeleteMapping("/{noteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long noteId) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        wrongNoteService.deleteNote(noteId, userId);
        return ResponseEntity.noContent().build();
    }

    /** 다중 삭제 */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BulkDeleteResponse> deleteBulk(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody List<Long> noteIds) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        BulkDeleteResponse res = wrongNoteService.deleteNotesBulk(noteIds, userId);
        return ResponseEntity.ok(res);
    }

    /** 별표 on/off 설정 */
    @PatchMapping("/{noteId}/star")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WrongNoteDto> setStar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long noteId,
            @RequestParam("starred") boolean starred) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        WrongNoteDto updated = wrongNoteService.setStarred(noteId, userId, starred);
        return ResponseEntity.ok(updated);
    }

    /** 별표 토글 */
    @PostMapping("/{noteId}/star/toggle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WrongNoteDto> toggleStar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long noteId) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        WrongNoteDto dto = wrongNoteService.toggleStar(noteId, userId);
        return ResponseEntity.ok(dto);
    }

    /** 별표 목록 */
    @GetMapping("/starred")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WrongNoteDto>> listStarred(
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        return ResponseEntity.ok(wrongNoteService.listStarredByUser(userId));
    }

    /** 오답 기반 퀴즈 시작(API) */
    @PostMapping("/quiz-start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QuizView> startQuizFromWrongNotes(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody WrongNoteQuizRequest req) {

        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long userId = principal.getId();

        QuizView view = quizService.generateFromWrongNotesForUser(userId, req);
        return ResponseEntity.ok(view);
    }

    /** DTO for partial update */
    public static class UpdateNoteRequest {
        private String note;
        private String starred; // 'Y' or 'N'
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getStarred() { return starred; }
        public void setStarred(String starred) { this.starred = starred; }
    }
}
