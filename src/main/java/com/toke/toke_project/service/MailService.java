package com.toke.toke_project.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    public void sendCodeHtml(String to, String code, int ttlMinutes) {
        try {
            log.info("📧 메일 전송 시작: to={}, code={}, ttl={}분", to, code, ttlMinutes);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");

            helper.setFrom("toke.japanese@gmail.com"); // 꼭 명시!
            helper.setTo(to);
            helper.setSubject("[TOKE] 인증 코드");

            // 템플릿 변수 세팅
            Context ctx = new Context();
            ctx.setVariable("code", code);
            ctx.setVariable("ttlMinutes", ttlMinutes);
            ctx.setVariable("email", to);

            // 템플릿 렌더링
            String html = templateEngine.process("mail/verification_code", ctx);
            helper.setText(html, true);

            mailSender.send(mime);

            log.info("✅ 메일 발송 성공: {}", to);

        } catch (Exception e) {
            log.error("❌ 메일 발송 실패: {}, 에러={}", to, e.getMessage(), e);
            throw new RuntimeException("메일 발송 실패", e);
        }
    }
}
