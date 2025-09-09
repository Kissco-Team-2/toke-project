package com.toke.toke_project.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    }
)
@Data
@SequenceGenerator(
        name = "userSeq",
        sequenceName = "seq_user_id",
        allocationSize = 1
)
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSeq")
    @Column(name = "user_id")
    private Long id;

    /** 표시용 이름(별칭). 로그인에는 사용하지 않음 */
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password; // 암호화된 비밀번호

    @Column(name = "phone_number", nullable = false, length = 20, unique = true)
    private String phoneNumber;

    /** 로그인 식별자 — Security에서 username으로 사용 */
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    /** 닉네임 — 화면용. 길이를 20으로 늘리고 싶으면 length=20 */
    @Column(nullable = false, length = 20, unique = true) // ← 필요 시 6 → 20으로 확장
    private String nickname;

    /** 권한 — 예: ROLE_USER, ROLE_ADMIN */
    @Column(nullable = false, length = 50)
    private String role = "ROLE_USER";

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        // role 기본값 보강
        if (role == null || role.isBlank()) role = "ROLE_USER";
        // email 정규화(소문자) — 선택
        if (email != null) email = email.trim().toLowerCase();
    }
}
