package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.UserLoginDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor //final 생성자 자동 생성
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistractionDto registractionDto){
        // ID 중복 체크
        if(authService.existsById(registractionDto.getId())){
            return new ResponseEntity<>("이미 존재하는 ID입니다.",HttpStatus.BAD_REQUEST);
        }
        // 휴대폰 번호 중복 체크
        if(authService.existsByPhone(registractionDto.getPhone())){
            return new ResponseEntity<>("이미 존재하는 휴대폰 번호 입니다.",HttpStatus.BAD_REQUEST);
        }
        // 비밀번호와 비밀번호 확인이 일치하는지 확인
        if(!registractionDto.getPw().equals(registractionDto.getConfirmPw())){
            return new ResponseEntity<>("비밀번호와 비밀번호 확인이 일치하지 않습니다.",HttpStatus.BAD_REQUEST);
        }
        // 모든 중복 및 유효성 검사 끝나면 서비스로 등록
        authService.registerUser(registractionDto);
        // 성공 메시지
        return new ResponseEntity<>("회원가입 성공", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<String> authenticateUser(@RequestBody UserLoginDto loginDto){
        try {
            authService.authenticate(loginDto.getId(), loginDto.getPw());

            return ResponseEntity.ok("로그인 성공");

        } catch (RuntimeException e) {
            // 인증 실패
            return new ResponseEntity<>(e.getMessage(),HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(){
        authService.logout();

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }


}
