package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 관리자 전용 API: 마이페이지, 가게 승인/거절, 회원 조회/제재, 리뷰 신고 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminService adminService;

    // 관리자 마이페이지 조회
    @GetMapping("/mypage")
    public ResponseEntity<?> getAdminProfile(Authentication authentication) {
        Integer userIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        try {
            MypageResponseDto dto = adminService.getAdminProfile(userIdx);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 관리자 마이페이지 정보 수정
    @PostMapping("/mypage/updateAdmin")
    public ResponseEntity<?> updateAdminProfile(@Valid @RequestBody AdminUpdateDto dto, Authentication authentication) {
        Integer userIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        adminService.updateAdminProfile(userIdx, dto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    // 승인 대기 중인 가게 목록 조회
    @GetMapping("/pending")
    public ResponseEntity<List<Restaurant>> getPendingList() {
        return ResponseEntity.ok(adminService.getPendingRestaurant());
    }

    // 가게 등록 승인
    @PostMapping("/approve/{restIdx}")
    public ResponseEntity<String> approveRestaurant(@PathVariable("restIdx") int restIdx) {
        adminService.approvalRestaurant(restIdx);
        return ResponseEntity.ok("가게가 성공적으로 승인되었습니다.");
    }

    // 가게 등록 거절
    @PostMapping("/reject/{restIdx}")
    public ResponseEntity<String> rejectRestaurant(@PathVariable("restIdx") int restIdx) {
        adminService.rejectRestaurant(restIdx);
        return ResponseEntity.ok("가게 등록이 거절되었습니다.");
    }

    // 전체 회원 목록 조회
    @GetMapping("/users")
    public ResponseEntity<List<UserListResponseDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // 특정 회원 상세 조회
    @GetMapping("/users/{userIdx}")
    public ResponseEntity<UserDetailsResponseDto> getUserDetails(@PathVariable("userIdx") int userIdx) {
        return ResponseEntity.ok(adminService.getUserDetails(userIdx));
    }

    // 회원 제재(사유 기록)
    @PostMapping("/users/{userIdx}/sanction")
    public ResponseEntity<String> sanctionUser(@PathVariable("userIdx") int userIdx,
                                               @RequestBody SanctionRequestDto requestDto) {
        try {
            adminService.sanctionUser(userIdx, requestDto.getReason());
            return ResponseEntity.ok("사용자가 성공적으로 제재되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 관리자에 의한 강제 탈퇴 처리(소프트 삭제)
    @PostMapping("/users/{userIdx}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable("userIdx") int userIdx) {
        try {
            adminService.deactivateUser(userIdx);
            return ResponseEntity.ok("사용자가 성공적으로 탈퇴 처리되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 전체 리뷰 신고 목록 조회
    @GetMapping("/reports")
    public ResponseEntity<List<ReviewReportResponseDto>> getAllReviewReports() {
        return ResponseEntity.ok(adminService.getAllReviewReports());
    }

    // 리뷰 신고 상세 조회
    @GetMapping("/reports/{reportIdx}")
    public ResponseEntity<ReviewReportResponseDto> getReviewReportDetails(@PathVariable("reportIdx") int reportIdx) {
        try {
            return ResponseEntity.ok(adminService.getReviewReportDetails(reportIdx));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 리뷰 신고 처리(승인 시 리뷰 삭제 등 후속 조치, approve 파라미터로 승인/반려 결정)
    @PostMapping("/reports/{reportIdx}/process")
    public ResponseEntity<String> processReviewReport(@PathVariable("reportIdx") int reportIdx,
                                                      @RequestParam("approve") boolean approve) {
        try {
            adminService.processReviewReport(reportIdx, approve);
            return ResponseEntity.ok("Review report processed successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
