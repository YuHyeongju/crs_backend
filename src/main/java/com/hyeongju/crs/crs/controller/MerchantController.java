package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.MerchantUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/mypage")
    public ResponseEntity<?> getMerchantProfile(HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("userIdx") == null){
            // request.getAttribute는 들어온 요청에서 id를 찾는것
            // session.getAttribute는 로그인 할 때 서버가 저장해둔 세션에서 찾는 것.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        }

        int userIdx = (int)session.getAttribute("userIdx");

        try {
            MypageResponseDto dto = merchantService.getMerchantProfile(userIdx);

            return ResponseEntity.ok(dto);
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/mypage/updateMerchant")
    public ResponseEntity<?> updateMerchantProfile(@RequestBody MerchantUpdateDto dto, HttpServletRequest request){
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute("userIdx") == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        int userIdx = (int)session.getAttribute("userIdx");

        merchantService.updateMerchantProfile(userIdx,dto);

        System.out.println("상인 정보 수정 완료");

        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }

}
