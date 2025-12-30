package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserLoginDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor //final 생성자 자동 생성
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST,
        RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/user")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistractionDto registractionDto){
        // ID 중복 체크
        if(authService.existsById(registractionDto.getId())){
            return new ResponseEntity<>("이미 존재하는 ID 입니다.",HttpStatus.BAD_REQUEST);
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

    @PostMapping("/register/merchant")
    public ResponseEntity<String> registerMerchant(@Valid @RequestBody MerchantRegistractionDto registractionDto){
        // ID 중복 체크
        if(authService.existsById(registractionDto.getId())){
            return new ResponseEntity<>("이미 존재하는 ID 입니다.",HttpStatus.BAD_REQUEST);
        }
        // 휴대폰 번호 중복 체크
        if(authService.existsByPhone(registractionDto.getPhone())){
            return new ResponseEntity<>("이미 존재하는 휴대폰 번호 입니다.",HttpStatus.BAD_REQUEST);
        }
        // 비밀번호와 비밀번호 확인이 일치하는지 확인
        if(!registractionDto.getPw().equals(registractionDto.getConfirmPw())){
            return new ResponseEntity<>("비밀번호와 비밀번호 확인이 일치하지 않습니다.",HttpStatus.BAD_REQUEST);
        }
        //사업자 등록번호 중복 체크
        if(authService.existsByBusinessNum(registractionDto.getBusinessNum())){
            return new ResponseEntity<>("이미 등록된 사업자 등록 번호 입니다.",HttpStatus.BAD_REQUEST);
        }

        // 모든 중복 및 유효성 검사 끝나면 서비스로 등록
        authService.registerMerchant(registractionDto);

        // 성공 메시지
        return new ResponseEntity<>("상인 회원가입 성공", HttpStatus.CREATED);
    }

    @PostMapping("/register/admin")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody AdminRegistractionDto registractionDto){
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
        //관리자 코드 중복 체크
        if(authService.existsByAdminNum(registractionDto.getAdminNum())){
            return new ResponseEntity<>("이미 등록된 관리자 코드 입니다.",HttpStatus.BAD_REQUEST);
        }

        // 모든 중복 및 유효성 검사 끝나면 서비스로 등록
        authService.registerAdmin(registractionDto);

        // 성공 메시지
        return new ResponseEntity<>("관리자 회원가입 성공", HttpStatus.CREATED);
    }


    @PostMapping("/login")
    public ResponseEntity<String> authenticateUser(@RequestBody UserLoginDto loginDto){
        try {
            authService.authenticate(loginDto.getId(), loginDto.getPw());

            System.out.println("로그인 성공");

            return ResponseEntity.ok("로그인 성공");


        } catch (RuntimeException e) {
            // 인증 실패
            System.out.println("로그인 실패");
            return new ResponseEntity<>(e.getMessage(),HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request){
        authService.logout(request);

        System.out.println("로그아웃 성공");
        
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestParam String id, HttpServletRequest request){
        authService.withdraw(id);

        HttpSession session = request.getSession(false);

        if(session != null){
            session.invalidate();
        }

        return ResponseEntity.ok("회원 탈퇴가 완료 되었습니다.");
    }

}
