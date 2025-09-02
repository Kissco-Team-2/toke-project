package com.toke.toke_project.repo;

import com.toke.toke_project.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepo extends JpaRepository<EmailVerification, Long> {

    // 최신 인증 코드 가져오기 (아직 안 쓴 것)
    Optional<EmailVerification> findTopByEmailAndPurposeAndConsumedOrderByCreatedAtDesc(
            String email, String purpose, String consumed
    );
}

