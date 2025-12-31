package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.AdminUpdateDto;
import com.hyeongju.crs.crs.dto.MerchantUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.dto.UserUpdateDto;
import com.hyeongju.crs.crs.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor // final이 붙은 필드의 생성자를 자동으로 만들어줌
public class UserController {

    private final UserService userService;


    @GetMapping("/mypage")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("id") == null){
            // request.getAttribute는 들어온 요청에서 id를 찾는것
            // session.getAttribute는 로그인 할 때 서버가 저장해둔 세션에서 찾는 것.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        }

        String id = (String)session.getAttribute("id");

        try {
            MypageResponseDto dto = userService.getMyProfile(id);

            return ResponseEntity.ok(dto);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/mypage/updateUser")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserUpdateDto dto,HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("id") == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String id = (String)session.getAttribute("id");

        userService.updateUserProfile(id,dto);

        System.out.println("일반 사용자 정보 수정 완료");

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
        
    }

    @PostMapping("/mypage/updateMerchant")
    public ResponseEntity<?> updateMerchantProfile(@RequestBody MerchantUpdateDto dto, HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("id") == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String id = (String)session.getAttribute("id");

        userService.updateMerchantProfile(id,dto);

        System.out.println("상인 정보 수정 완료");

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    @PostMapping("/mypage/updateAdmin")
    public ResponseEntity<?> updateAdminProfile(@RequestBody AdminUpdateDto dto, HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("id") == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String id = (String)session.getAttribute("id");

        userService.updateAdminProfile(id,dto);

        System.out.println("관리자 정보 수정 완료");

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");

    }







}
