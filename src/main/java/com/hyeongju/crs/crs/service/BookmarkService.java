package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.BookMark;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BookMarkDto;
import com.hyeongju.crs.crs.repository.BookMarkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookMarkRepository bookMarkRepository;
    private final RestaurantService restaurantService;

    @Transactional
    public String toggleBookMark(User user, BookMarkDto dto){

        System.out.println(">>> [북마크] kakaoId=" + dto.getKakaoId() + " restIdx=" + dto.getRestIdx());

        Restaurant newRestaurant;
        if (dto.getRestIdx() != null) {
            newRestaurant = restaurantService.getRestaurantByRestIdx(dto.getRestIdx());
        } else if (dto.getKakaoId() != null) {
            newRestaurant = restaurantService.getOrCreateRestaurant(
                    dto.getKakaoId(), dto.getRestName(), dto.getRestAddress(), dto.getRestTel());
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
        List<BookMark> bookmarks = bookMarkRepository.findByUserUserIdx(userIdx);

        return bookmarks.stream().map(bm -> {
            String kakaoId = bm.getRestaurant().getKakaoId();
            return kakaoId != null ? kakaoId : "db-" + bm.getRestaurant().getRestIdx();
        }).collect(Collectors.toList());
    }

    public List<BookMarkDto> getBookmarkListForMypage(int userIdx) {
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
