package com.hyeongju.crs.crs.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {
    // 이메일 인증코드를 메모리에 임시 저장하고 검증하는 서비스 (별도 DB 테이블 없이 서버 메모리만 사용)
    // 주의: 서버가 여러 대(다중 인스턴스)로 뜨면 인스턴스마다 저장소가 따로 놀아 인증이 깨질 수 있음 — 단일 서버 기준 구현

    private final ConcurrentHashMap<String, VerificationEntry> store = new ConcurrentHashMap<>();
    // key: 용도+이메일 조합 문자열(예: "findId:abc@test.com"), value: 발급된 코드와 만료시각
    private final Random random = new Random();

    public String generateAndStore(String key) {
        String code = String.format("%06d", random.nextInt(1_000_000)); // 000000~999999 사이 6자리 숫자 코드 생성
        store.put(key, new VerificationEntry(code, LocalDateTime.now().plusMinutes(5))); // 5분 뒤 만료로 저장
        return code;
    }

    public boolean verify(String key, String code) {
        VerificationEntry entry = store.get(key);
        if (entry == null) return false; // 발급 이력이 없음
        if (LocalDateTime.now().isAfter(entry.expiry())) {
            store.remove(key); // 만료된 코드는 정리하고 실패 처리
            return false;
        }
        if (!entry.code().equals(code)) return false; // 코드 불일치
        store.remove(key); // 검증에 성공하면 재사용 방지를 위해 즉시 삭제(1회용)
        return true;
    }

    private record VerificationEntry(String code, LocalDateTime expiry) {}
    // 코드와 만료시각을 함께 담는 불변 값 객체
}
