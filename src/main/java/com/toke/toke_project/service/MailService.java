package com.toke.toke_project.service;
//인증 코드 메일 발송 (JavaMailSender 사용)
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    /**
     * 간단한 텍스트 메일 발송
     */
    public void sendCode(String to, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("[TOKE] 인증 코드");
        msg.setText("인증 코드는 " + code + " 입니다. 10분 내 입력해주세요.");
        mailSender.send(msg);
    }
}
