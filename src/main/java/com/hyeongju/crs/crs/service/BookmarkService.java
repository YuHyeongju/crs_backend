package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.BookMark;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BookMarkDto;
import com.hyeongju.crs.crs.repository.BookMarkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    // 북마크 등록/해제(토글) 및 조회를 담당하는 서비스
    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);

    private final BookMarkRepository bookMarkRepository;
    private final RestaurantService restaurantService;

    @Transactional
    public String toggleBookMark(User user, BookMarkDto dto){
        // 이미 북마크되어 있으면 해제(delete), 아니면 새로 등록(add) — 하나의 API로 두 동작을 겸함

        log.debug("북마크 토글: kakaoId={}, restIdx={}", dto.getKakaoId(), dto.getRestIdx());

        Restaurant newRestaurant;
        if (dto.getRestIdx() != null) {
            newRestaurant = restaurantService.getRestaurantByRestIdx(dto.getRestIdx());
        } else if (dto.getKakaoId() != null) {
            newRestaurant = restaurantService.getOrCreateRestaurant(
                    dto.getKakaoId(), dto.getRestName(), dto.getRestAddress(), dto.getRestTel());
            // 카카오 지도에서만 존재하는 식당이면 DB에 새로 생성
        } else {
            throw new IllegalArgumentException("restIdx 또는 kakaoId 중 하나는 필수입니다.");
        }

        Optional<BookMark> existingBookMark = bookMarkRepository.findByUserAndRestaurant(user,newRestaurant);

        if(existingBookMark.isPresent()){
            bookMarkRepository.delete(existingBookMark.get());
            return "delete";
        }else{
            BookMark bm = new BookMark();
            bm.setUser(user);
            bm.setRestaurant(newRestaurant);
            bm.setBookMarkedAt(LocalDateTime.now());

            bookMarkRepository.save(bm);
        }
        return "add";
    }
    public List<String> getBookmarkKakaoIds(int userIdx){
        // 지도 화면에서 "북마크된 식당인지" 표시하기 위해 kakaoId 목록만 뽑아 반환
        List<BookMark> bookmarks = bookMarkRepository.findByUserUserIdx(userIdx);

        return bookmarks.stream().map(bm -> {
            String kakaoId = bm.getRestaurant().getKakaoId();
            return kakaoId != null ? kakaoId : "db-" + bm.getRestaurant().getRestIdx();
            // 카카오 식별자가 없는(상인이 직접 등록한) 식당은 "db-restIdx" 형태의 가짜 식별자로 대체
        }).collect(Collectors.toList());
    }

    public List<BookMarkDto> getBookmarkListForMypage(int userIdx) {
        // 마이페이지 "내 북마크 목록"에 필요한 상세 정보(이름/주소/전화 등)까지 포함해서 반환
        List<BookMark> bookMarks = bookMarkRepository.findByUserUserIdx(userIdx);

        return bookMarks.stream().map(bm -> {
            BookMarkDto dto = new BookMarkDto();
            dto.setKakaoId(bm.getRestaurant().getKakaoId());
            dto.setRestIdx(bm.getRestaurant().getRestIdx());
            dto.setRestName(bm.getRestaurant().getRestName());
            dto.setRestAddress(bm.getRestaurant().getRestAddress());
            dto.setRestTel(bm.getRestaurant().getRestTel());
            dto.setUserIdx(userIdx);
            return dto;
        }).collect(Collectors.toList());
    }
}
