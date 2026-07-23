package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/mypage")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            MypageResponseDto dto = userService.getUserProfile(userIdx);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/mypage/updateUser")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UserUpdateDto dto, HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        userService.updateUserProfile(userIdx, dto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    @PostMapping("/find-id/send-code")
    public ResponseEntity<?> sendFindIdCode(@Valid @RequestBody FindIdSendCodeDto dto) {
        try {
            userService.sendFindIdCode(dto.getName(), dto.getEmail());
            return ResponseEntity.ok("인증번호가 이메일로 발송되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/find-id/verify")
    public ResponseEntity<?> verifyFindId(@Valid @RequestBody FindIdVerifyDto dto) {
        try {
            String maskedId = userService.verifyFindId(dto.getName(), dto.getEmail(), dto.getCode());
            return ResponseEntity.ok(maskedId);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password/send-code")
    public ResponseEntity<?> sendResetPasswordCode(@Valid @RequestBody ResetPasswordSendCodeDto dto) {
        try {
            userService.sendResetPasswordCode(dto.getId(), dto.getEmail());
            return ResponseEntity.ok("인증번호가 이메일로 발송되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/reset-password/verify")
    public ResponseEntity<?> verifyAndResetPassword(@Valid @RequestBody ResetPasswordVerifyDto dto) {
        try {
            userService.verifyAndResetPassword(dto.getId(), dto.getEmail(), dto.getCode(), dto.getNewPassword());
            return ResponseEntity.ok("비밀번호가 변경되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
