package com.toke.toke_project.web;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.service.QuizService;
import com.toke.toke_project.service.WrongNoteService;
import com.toke.toke_project.web.dto.BulkDeleteResponse;
import com.toke.toke_project.web.dto.QuizView;
import com.toke.toke_project.web.dto.WrongNoteDto;
import com.toke.toke_project.web.dto.WrongNoteQuizRequest;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/wrong-notes")
public class WrongNoteController {

    private final WrongNoteService wrongNoteService;
    private final UsersRepository usersRepository;
    private final QuizService quizService;     
    
    public WrongNoteController(WrongNoteService wrongNoteService, UsersRepository usersRepository, QuizService quizService) {
        this.wrongNoteService = wrongNoteService;
        this.usersRepository = usersRepository;
        this.quizService = quizService;
    }

    // --- 도움 메서드: Principal에서 username을 꺼내 users 테이블에서 id를 얻음 ---
    private Long resolveUserId(Principal principal) {
        if (principal == null) return null;
        String username = principal.getName();
        return usersRepository.findByUsername(username)
                .map(Users::getId)
                .orElse(null);
    }

    // 간단 목록 (기본 최신순) - 로그인 사용자 기준
    @GetMapping
    public ResponseEntity<List<WrongNoteDto>> list(Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(wrongNoteService.listByUser(userId));
    }

    // 목록 (필터 포함) — 로그인 사용자 기준
    @GetMapping("/filter")
    public ResponseEntity<Page<WrongNoteDto>> listFiltered(
            Principal principal,
            @RequestParam(value = "sort", defaultValue = "LATEST") String sort,
            @RequestParam(value = "dateFilter", defaultValue = "ALL") String dateFilter,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Page<WrongNoteDto> res = wrongNoteService.listByUserWithFilters(userId, sort, dateFilter, from, to, category, page, size);
        return ResponseEntity.ok(res);
    }

    // 단건 업데이트 (메모 작성/수정 & starred)
    @PatchMapping("/{noteId}")
    public ResponseEntity<WrongNoteDto> updateNote(
            Principal principal,
            @PathVariable Long noteId,
            @RequestBody UpdateNoteRequest req
    ) {
        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 시연 목적: 간단 호출 (원하면 서비스에 소유권 체크 메서드로 바꿔 드립니다)
        WrongNoteDto dto = wrongNoteService.updateNote(noteId, req.getNote(), req.getStarred());
        return ResponseEntity.ok(dto);
    }

    // 단건 삭제 (noteId) - 시연용: deletes without explicit ownership check at controller level
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable Long noteId, Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 권한 검증이 서비스에 없다면 (권장) 서비스에서 체크할 수 있게 바꾸세요.
        wrongNoteService.deleteNote(noteId);
        return ResponseEntity.noContent().build();
    }

    // 다중 삭제 (body: [id, id, ...]) - 로그인 사용자 기준으로 소유한 것만 삭제 처리
    @DeleteMapping
    public ResponseEntity<BulkDeleteResponse> deleteBulk(
            Principal principal,
            @RequestBody List<Long> noteIds) {

        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        BulkDeleteResponse res = wrongNoteService.deleteNotesBulk(noteIds, userId);
        return ResponseEntity.ok(res);
    }

    // set starred (explicit Y/N)
    @PatchMapping("/{noteId}/star")
    public ResponseEntity<WrongNoteDto> setStar(
            Principal principal,
            @PathVariable Long noteId,
            @RequestParam("starred") boolean starred) {

        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        WrongNoteDto updated = wrongNoteService.setStarred(noteId, userId, starred);
        return ResponseEntity.ok(updated);
    }

    // toggle star
    @PostMapping("/{noteId}/star/toggle")
    public ResponseEntity<WrongNoteDto> toggleStar(Principal principal, @PathVariable Long noteId) {
        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        WrongNoteDto dto = wrongNoteService.toggleStar(noteId, userId);
        return ResponseEntity.ok(dto);
    }

    // starred 목록
    @GetMapping("/starred")
    public ResponseEntity<List<WrongNoteDto>> listStarred(Principal principal) {
        Long userId = resolveUserId(principal);
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(wrongNoteService.listStarredByUser(userId));
    }

    // DTO for partial update
    public static class UpdateNoteRequest {
        private String note;
        private String starred; // 'Y' or 'N'

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public String getStarred() { return starred; }
        public void setStarred(String starred) { this.starred = starred; }
    }
    
 // 컨트롤러 내부에 추가
    @PostMapping("/quiz-start")
    public ResponseEntity<QuizView> startQuizFromWrongNotes(Principal principal,
            @RequestBody WrongNoteQuizRequest req) {

        Long userId = resolveUserId(principal); // 기존 resolveUserId helper 사용
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        QuizView view = quizService.generateFromWrongNotesForUser(userId, req);
        return ResponseEntity.ok(view);
    }
}
