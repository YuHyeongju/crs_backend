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
public class BookMarkService {
    private final BookMarkRepository bookMarkRepository;
    private final RestaurantService restaurantService;

    @Transactional
    public String toggleBookMark(User user, BookMarkDto dto){


        Restaurant newRestaurant = restaurantService.getOrCreateRestaurant(
                dto.getKakaoId(),
                dto.getRestName(),
                dto.getRestAddress(),
                dto.getRestTel()
        );

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

        return bookmarks.stream().map(bm -> bm.getRestaurant().getKakaoId())
                .collect(Collectors.toList());
    }

}
