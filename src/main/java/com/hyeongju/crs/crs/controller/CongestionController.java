package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.CongestionUpdateDto;
import com.hyeongju.crs.crs.dto.MyCongestionResponseDto;
import com.hyeongju.crs.crs.service.CongestionService;
import jakarta.servlet.http.HttpSession;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
@Builder // 빌더 패턴을 사용할 수 있게 해줌

public class CongestionController {

    private final CongestionService congestionService;

    @GetMapping("/{kakaoId}")
    public ResponseEntity<String> getCurrentCongestion(@PathVariable("kakaoId") String kakaoId){
        String currentStatus = congestionService.getCurrentcongestion(kakaoId);
        System.out.println("===================================================");
        System.out.println("단일 가게 혼잡도 조회 완료");
        System.out.println("===================================================");
        return ResponseEntity.ok(currentStatus);
    }

    @PostMapping("/bulkStatus")
    public ResponseEntity<Map<String, String>> getCurrentCongstionAll(@RequestBody List<String> kakaoIds){
        System.out.println("==========================================================");
        System.out.println("전체 가게 혼잡도 조회 완료 ");
        System.out.println("==========================================================");
        return ResponseEntity.ok(congestionService.getAllCurrentCongestion(kakaoIds));
    }

    @PostMapping("/updateStatus")
    public ResponseEntity<Void> updateCongestion(@RequestBody CongestionUpdateDto dto){
        System.out.println("전달 받은 카카오 ID: " + dto.getKakaoId());
        System.out.println("전달 받은 혼잡도 상태: " + dto.getCongStatus());
        System.out.println("전달 받은 userIdx: " + dto.getUserIdx());
        System.out.println("전달 받은 식당 이름: " + dto.getRestName());
        System.out.println("전달 받은 식당 주소: " + dto.getRestAddress());
        System.out.println("전달 받은 식당 전화번호: " + dto.getRestPhone());
        congestionService.changeCongStatus(dto);
        System.out.println("혼잡도 상태 업데이트 완료");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<MyCongestionResponseDto>> getMyHistory(HttpSession session){
        Integer userIdx= (Integer)session.getAttribute("userIdx");

        if(userIdx == null){
            System.out.println("세션에 userIdx가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<MyCongestionResponseDto> result = congestionService.getMyCongestionHistory(userIdx);
        return ResponseEntity.ok(result);

    }
}
