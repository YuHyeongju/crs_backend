package com.hyeongju.crs.crs.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 모든 컨트롤러에서 밖으로 새어나온 예외를 한곳에서 가로채, 일관된 형태의 응답으로 바꿔주는 전역 처리기.
    // 응답 본문을 String으로 통일한 이유: 기존 컨트롤러들이 전부 ResponseEntity<String>을 반환하고,
    // 프론트도 err.response.data를 그대로 화면에 렌더링하기 때문(객체를 내려주면 React에서 렌더링 에러가 남).

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException e) {
        // @Valid 검증 실패 시 스프링이 던지는 예외.
        // 컨트롤러 메서드 본문이 실행되기 "전"에 발생하므로, 컨트롤러 안의 try-catch로는 잡을 수 없어 여기서만 처리 가능함.
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage) // DTO의 @NotBlank(message = "...")에 적어둔 한글 메시지
                .findFirst()                        // 여러 개 실패해도 배너에 한 줄만 보여주면 되므로 첫 번째만 사용
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleBadRequest(RuntimeException e) {
        // 서비스 계층에서 잘못된 입력을 알릴 때 던지는 예외들 - 서버 잘못(500)이 아니라 요청 잘못(400)으로 응답
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException e) {
        // 권한 부족은 아래 handleUnexpected(500)로 흘러가면 안 되므로 403으로 따로 처리
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception e) {
        // 예상하지 못한 예외 - 원본 메시지에는 SQL 문이나 테이블/컬럼명 같은 내부 구조가 담길 수 있어
        // 클라이언트에는 일반적인 문구만 내려주고, 실제 원인은 서버 로그에만 남김
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
}
