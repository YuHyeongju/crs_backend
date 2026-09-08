package com.hyeongju.crs.crs.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("@Valid 검증 실패는 첫 번째 필드 메시지와 함께 400 으로 변환된다")
    void handleValidation_usesFirstFieldMessage() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new org.springframework.validation.FieldError(
                "dto", "id", "아이디는 필수 입력 값입니다."));
        bindingResult.addError(new org.springframework.validation.FieldError(
                "dto", "pw", "비밀번호는 필수 입력 값입니다."));

        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        given(e.getBindingResult()).willReturn(bindingResult);

        ResponseEntity<String> response = handler.handleValidation(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("아이디는 필수 입력 값입니다.");
    }

    @Test
    @DisplayName("필드 에러가 없으면 기본 메시지를 사용한다")
    void handleValidation_fallbackMessage() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");

        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        given(e.getBindingResult()).willReturn(bindingResult);

        ResponseEntity<String> response = handler.handleValidation(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("입력값이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("IllegalArgumentException 은 메시지 그대로 400 으로 변환된다")
    void handleBadRequest_illegalArgument() {
        ResponseEntity<String> response =
                handler.handleBadRequest(new IllegalArgumentException("이미 존재하는 아이디입니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("IllegalStateException 도 400 으로 변환된다")
    void handleBadRequest_illegalState() {
        ResponseEntity<String> response =
                handler.handleBadRequest(new IllegalStateException("가게를 찾을 수 없습니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("가게를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("권한 부족은 403 과 고정 문구로 응답한다")
    void handleAccessDenied() {
        ResponseEntity<String> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("예상치 못한 예외는 내부 정보를 감춘 채 500 으로 응답한다")
    void handleUnexpected_hidesInternalDetails() {
        ResponseEntity<String> response =
                handler.handleUnexpected(new RuntimeException("SELECT * FROM user WHERE ..."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        assertThat(response.getBody()).doesNotContain("SELECT");
    }
}
