package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.AdminUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admins")

public class AdminController {

    private final AdminService adminService;

    @GetMapping("/mypage")
    public ResponseEntity<?> getAdminProfile(HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("userIdx") == null){
            // request.getAttribute는 들어온 요청에서 id를 찾는것
            // session.getAttribute는 로그인 할 때 서버가 저장해둔 세션에서 찾는 것.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        }

        int userIdx = (int)session.getAttribute("userIdx");

        try {
            MypageResponseDto dto = adminService.getAdminProfile(userIdx);

            return ResponseEntity.ok(dto);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/mypage/updateAdmin")
    public ResponseEntity<?> updateAdminProfile(@RequestBody AdminUpdateDto dto, HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("userIdx") == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        int userIdx = (int)session.getAttribute("userIdx");

        adminService.updateAdminProfile(userIdx,dto);

        System.out.println("관리자 정보 수정 완료");

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");

    }
    @GetMapping("/pending")
    public ResponseEntity<List<Restaurant>> getPendingList(){
        return ResponseEntity.ok(adminService.getPendingRestaurant());
    }


    @PostMapping("/approve/{restIdx}")
    public ResponseEntity<String> approveRestaurant(@PathVariable ("restIdx") int restIdx){
        adminService.approvalRestaurant(restIdx);
        return ResponseEntity.ok("가게가 성공적으로 승인되었습니다.");
    }

    @PostMapping("/reject/{restIdx}")
    public ResponseEntity<String> rejectRestaurant(@PathVariable ("restIdx") int restIdx){
        adminService.rejectRestaurant(restIdx);
        return ResponseEntity.ok("가게 등록이 거절되었습니다.");
    }
}
