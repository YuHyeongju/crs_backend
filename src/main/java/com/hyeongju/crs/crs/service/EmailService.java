package com.hyeongju.crs.crs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("yuheoungju159@gmail.com");
        message.setTo(to);
        message.setSubject("[CRS] 인증번호 안내");
        message.setText("인증번호: " + code + "\n\n유효시간은 5분입니다.");
        mailSender.send(message);
    }
}
