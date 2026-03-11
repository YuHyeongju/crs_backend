package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BookMarkDto;
import com.hyeongju.crs.crs.repository.UserRepository;
import com.hyeongju.crs.crs.service.BookmarkService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final UserRepository userRepository;
    private final BookmarkService bookMarkService;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleBookMark(@RequestBody BookMarkDto dto){

        User user = userRepository.findById(dto.getUserIdx())
                .orElseThrow(()-> new IllegalStateException("해당 하는 유저 없음"));

        String result = bookMarkService.toggleBookMark(user, dto);

        if("add".equals(result)) {
            System.out.println("즐겨찾기 추가");
        }else if("delete".equals(result)){
            System.out.println("즐겨찾기 해제");
            }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-bookmark-list/{userIdx}")
    public ResponseEntity<List<String>> getUserBookMarkIds(@PathVariable("userIdx") int userIdx) {
        // 🔍 1. 요청이 들어왔는지 확인
        System.out.println("====== [북마크 조회 요청] userIdx: " + userIdx + " ======");

        List<String> bookmarkIds = bookMarkService.getBookmarkKakaoIds(userIdx);

        // 🔍 2. 실제 DB에서 꺼내온 데이터가 있는지 확인
        if (bookmarkIds == null || bookmarkIds.isEmpty()) {
            System.out.println(">>> 결과: 즐겨찾기 목록이 비어있습니다.");
        } else {
            System.out.println(">>> 결과: " + bookmarkIds.size() + "개의 아이디를 찾았습니다.");
            System.out.println(">>> 데이터: " + bookmarkIds);
        }

        return ResponseEntity.ok(bookmarkIds);
    }
    @GetMapping("/details")
    public ResponseEntity<List<BookMarkDto>> getMyBookmarkDetails(HttpSession session){
        Integer userIdx = (Integer) session.getAttribute("userIdx");

        if(userIdx == null){
            return ResponseEntity.status(401).build();
        }

        List<BookMarkDto> bookmarkDetails = bookMarkService.getBookmarkListForMypage(userIdx);

        System.out.println("즐겨찾기한 가게 수: " + bookmarkDetails.size());

        return ResponseEntity.ok(bookmarkDetails);
    }
}
