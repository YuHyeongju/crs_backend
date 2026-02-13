package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.AdminUpdateDto;
import com.hyeongju.crs.crs.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admins")

public class AdminController {

    private final AdminService adminService;

    @PostMapping("/mypage/updateAdmin")
    public ResponseEntity<?> updateAdminProfile(@RequestBody AdminUpdateDto dto, HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("id") == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String id = (String)session.getAttribute("id");

        adminService.updateAdminProfile(id,dto);

        System.out.println("관리자 정보 수정 완료");

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");

    }
}
