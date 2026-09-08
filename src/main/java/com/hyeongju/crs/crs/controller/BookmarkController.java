package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BookMarkDto;
import com.hyeongju.crs.crs.repository.UserRepository;
import com.hyeongju.crs.crs.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    // 북마크 등록/해제(토글) 및 조회 API
    // 다른 컨트롤러와 달리 UserRepository를 직접 주입받아 사용(서비스 계층을 한 번 더 거치지 않음)

    private final UserRepository userRepository;
    private final BookmarkService bookMarkService;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleBookMark(@RequestBody BookMarkDto dto, Authentication authentication){
        // 북마크 토글 API — 이미 등록돼 있으면 해제, 아니면 등록
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        User user = userRepository.findById(authedUserIdx)
                .orElseThrow(()-> new IllegalStateException("해당 하는 유저 없음"));

        String result = bookMarkService.toggleBookMark(user, dto); // "add" 또는 "delete" 문자열로 결과 반환

        if("add".equals(result)) {
            System.out.println("즐겨찾기 추가");
        }else if("delete".equals(result)){
            System.out.println("즐겨찾기 해제");
            }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-bookmark-list/{userIdx}")
    public ResponseEntity<List<String>> getUserBookMarkIds(@PathVariable("userIdx") int userIdx,
                                                            Authentication authentication) {
        // 로그인한 유저의 북마크 kakaoId 목록 조회 API (지도에서 북마크 표시용)
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(401).build();
        }
        System.out.println("====== [북마크 조회 요청] userIdx: " + authedUserIdx + " ======");
        // 경로변수 userIdx는 사용되지 않음 — 실제 조회는 토큰의 authedUserIdx 기준

        List<String> bookmarkIds = bookMarkService.getBookmarkKakaoIds(authedUserIdx);

        if (bookmarkIds == null || bookmarkIds.isEmpty()) {
            System.out.println(">>> 결과: 즐겨찾기 목록이 비어있습니다.");
        } else {
            System.out.println(">>> 결과: " + bookmarkIds.size() + "개의 아이디를 찾았습니다.");
            System.out.println(">>> 데이터: " + bookmarkIds);
        }

        return ResponseEntity.ok(bookmarkIds);
    }
    @GetMapping("/details")
    public ResponseEntity<List<BookMarkDto>> getMyBookmarkDetails(Authentication authentication){
        // 마이페이지 "내 북마크 목록"용 상세 정보 조회 API
        Integer userIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;

        if(userIdx == null){
            return ResponseEntity.status(401).build();
        }

        List<BookMarkDto> bookmarkDetails = bookMarkService.getBookmarkListForMypage(userIdx);

        System.out.println("즐겨찾기한 가게 수: " + bookmarkDetails.size());

        return ResponseEntity.ok(bookmarkDetails);
    }
}
