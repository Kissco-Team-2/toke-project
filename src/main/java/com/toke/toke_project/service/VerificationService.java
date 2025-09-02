package com.toke.toke_project.service;
//인증 코드 생성/저장/검증 (DB 테이블 email_verification 사용)
import com.toke.toke_project.domain.EmailVerification;
import com.toke.toke_project.repo.EmailVerificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final EmailVerificationRepo repo;

    /**
     * 인증 코드 생성 → DB 저장 → 외부 발송자(메일 등)로 전달
     * @param ttl 유효 시간 (예: Duration.ofMinutes(10))
     * @param sender 코드 전달 함수(예: code -> mailService.sendCode(email, code))
     */
    @Transactional
    public void createAndSend(String email, String purpose, Duration ttl,
                              java.util.function.Consumer<String> sender) {

        String code = String.format("%06d",
                ThreadLocalRandom.current().nextInt(0, 1_000_000));

        EmailVerification ev = new EmailVerification();
        ev.setEmail(email);
        ev.setPurpose(purpose);                // "RESET_PASSWORD" / "SIGNUP_VERIFY" 등
        ev.setCode(code);
        ev.setExpiresAt(LocalDateTime.now().plus(ttl));
        ev.setConsumed("N");

        repo.save(ev);                         // DB 저장 후
        sender.accept(code);                   // 실제 발송 (메일/SMS 등)
    }

    /**
     * 코드 검증: 만료/불일치/이미 사용 여부 체크 후, 사용 처리(consumed='Y')
     */
    @Transactional
    public void verify(String email, String purpose, String code) {
        EmailVerification ev = repo
                .findTopByEmailAndPurposeAndConsumedOrderByCreatedAtDesc(email, purpose, "N")
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 먼저 해주세요."));

        if (LocalDateTime.now().isAfter(ev.getExpiresAt())) {
            throw new IllegalStateException("인증 코드가 만료되었습니다.");
        }
        if (!ev.getCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }
        ev.setConsumed("Y");  // 일회성 사용
        // JPA 영속 상태이므로 트랜잭션 종료 시 자동 업데이트
    }
}

