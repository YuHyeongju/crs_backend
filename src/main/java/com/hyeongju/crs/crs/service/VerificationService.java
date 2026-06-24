package com.hyeongju.crs.crs.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    private final ConcurrentHashMap<String, VerificationEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateAndStore(String key) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(key, new VerificationEntry(code, LocalDateTime.now().plusMinutes(5)));
        return code;
    }

    public boolean verify(String key, String code) {
        VerificationEntry entry = store.get(key);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry())) {
            store.remove(key);
            return false;
        }
        if (!entry.code().equals(code)) return false;
        store.remove(key);
        return true;
    }

    private record VerificationEntry(String code, LocalDateTime expiry) {}
}
