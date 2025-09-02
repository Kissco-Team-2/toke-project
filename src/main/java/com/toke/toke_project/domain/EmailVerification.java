package com.toke.toke_project.domain;
//email_verification 테이블 매핑 (회원가입 확인/비번 재설정 코드 관리)
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Data
@SequenceGenerator(
        name = "emailVerSeq",
        sequenceName = "seq_email_verification",
        allocationSize = 1
)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emailVerSeq")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 10)
    private String code; // 인증 코드

    @Column(nullable = false, length = 30)
    private String purpose; // SIGNUP_VERIFY / RESET_PASSWORD

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, length = 1)
    private String consumed = "N"; // Y / N
}
