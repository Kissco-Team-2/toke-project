package com.toke.toke_project.domain;
//users 테이블 매핑 (회원 정보 저장)
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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

    @Column(nullable = false, length = 100)
    private String username; // 사용자 이름

    @Column(nullable = false, length = 255)
    private String password; // 암호화된 비밀번호

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 6, unique = true)
    private String nickname;

    @Column(nullable = false, length = 50)
    private String role = "user"; // 기본 user

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}
