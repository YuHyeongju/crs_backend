package com.hyeongju.crs.crs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인증코드 저장소는 메모리(ConcurrentHashMap) 기반이라 목킹 없이 실제 객체로 검증한다.
 */
class VerificationServiceTest {

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService();
    }

    @Test
    @DisplayName("인증코드는 6자리 숫자로 생성된다")
    void generateAndStore_returnsSixDigitCode() {
        String code = verificationService.generateAndStore("findId:a@b.com");

        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}");
    }

    @Test
    @DisplayName("발급된 코드로 검증하면 성공한다")
    void verify_success() {
        String code = verificationService.generateAndStore("findId:a@b.com");

        assertThat(verificationService.verify("findId:a@b.com", code)).isTrue();
    }

    @Test
    @DisplayName("코드가 다르면 검증 실패")
    void verify_wrongCode() {
        verificationService.generateAndStore("findId:a@b.com");

        assertThat(verificationService.verify("findId:a@b.com", "000000-wrong")).isFalse();
    }

    @Test
    @DisplayName("발급 이력이 없는 키는 검증 실패")
    void verify_unknownKey() {
        assertThat(verificationService.verify("findId:none@b.com", "123456")).isFalse();
    }

    @Test
    @DisplayName("검증에 성공한 코드는 1회용이라 재사용할 수 없다")
    void verify_isOneTimeUse() {
        String code = verificationService.generateAndStore("resetPw:a@b.com");

        assertThat(verificationService.verify("resetPw:a@b.com", code)).isTrue();
        assertThat(verificationService.verify("resetPw:a@b.com", code)).isFalse();
    }

    @Test
    @DisplayName("같은 키로 다시 발급하면 이전 코드는 무효가 된다")
    void generateAndStore_overwritesPreviousCode() {
        String first = verificationService.generateAndStore("findId:a@b.com");
        String second = verificationService.generateAndStore("findId:a@b.com");

        // 매우 낮은 확률로 같은 난수가 나올 수 있으므로 다를 때만 이전 코드 무효를 검증한다
        if (!first.equals(second)) {
            assertThat(verificationService.verify("findId:a@b.com", first)).isFalse();
        }
        assertThat(verificationService.verify("findId:a@b.com", second)).isTrue();
    }

    @Test
    @DisplayName("만료된 코드는 검증 실패하고 저장소에서 제거된다")
    @SuppressWarnings("unchecked")
    void verify_expiredCode() throws Exception {
        Map<String, Object> store =
                (Map<String, Object>) ReflectionTestUtils.getField(verificationService, "store");
        assertThat(store).isNotNull();

        Class<?> entryClass = Class.forName(
                "com.hyeongju.crs.crs.service.VerificationService$VerificationEntry");
        Constructor<?> constructor = entryClass.getDeclaredConstructor(String.class, LocalDateTime.class);
        constructor.setAccessible(true);
        Object expiredEntry = constructor.newInstance("123456", LocalDateTime.now().minusMinutes(1));

        store.put("findId:expired@b.com", expiredEntry);

        assertThat(verificationService.verify("findId:expired@b.com", "123456")).isFalse();
        assertThat(store).doesNotContainKey("findId:expired@b.com");
    }
}
