package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> getAdminProfile(HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        try {
            MypageResponseDto dto = adminService.getAdminProfile(userIdx);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/mypage/updateAdmin")
    public ResponseEntity<?> updateAdminProfile(@RequestBody AdminUpdateDto dto, HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        adminService.updateAdminProfile(userIdx, dto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Restaurant>> getPendingList() {
        return ResponseEntity.ok(adminService.getPendingRestaurant());
    }

    @PostMapping("/approve/{restIdx}")
    public ResponseEntity<String> approveRestaurant(@PathVariable("restIdx") int restIdx) {
        adminService.approvalRestaurant(restIdx);
        return ResponseEntity.ok("가게가 성공적으로 승인되었습니다.");
    }

    @PostMapping("/reject/{restIdx}")
    public ResponseEntity<String> rejectRestaurant(@PathVariable("restIdx") int restIdx) {
        adminService.rejectRestaurant(restIdx);
        return ResponseEntity.ok("가게 등록이 거절되었습니다.");
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserListResponseDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userIdx}")
    public ResponseEntity<UserDetailsResponseDto> getUserDetails(@PathVariable("userIdx") int userIdx) {
        return ResponseEntity.ok(adminService.getUserDetails(userIdx));
    }

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

    @PostMapping("/users/{userIdx}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable("userIdx") int userIdx) {
        try {
            adminService.deactivateUser(userIdx);
            return ResponseEntity.ok("사용자가 성공적으로 탈퇴 처리되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ReviewReportResponseDto>> getAllReviewReports() {
        return ResponseEntity.ok(adminService.getAllReviewReports());
    }

    @GetMapping("/reports/{reportIdx}")
    public ResponseEntity<ReviewReportResponseDto> getReviewReportDetails(@PathVariable("reportIdx") int reportIdx) {
        try {
            return ResponseEntity.ok(adminService.getReviewReportDetails(reportIdx));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

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
