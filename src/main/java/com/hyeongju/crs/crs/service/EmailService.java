package com.hyeongju.crs.crs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    // 아이디 찾기/비밀번호 재설정 등에 쓰이는 인증코드 이메일 발송 서비스

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        // 인증코드를 담은 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("yuheoungju159@gmail.com");
        message.setTo(to);
        message.setSubject("[CRS] 인증번호 안내");
        message.setText("인증번호: " + code + "\n\n유효시간은 5분입니다.");
        mailSender.send(message);
    }
}
