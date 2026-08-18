package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.BookMark;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookMarkRepository extends JpaRepository<BookMark, Integer> {

    // 특정 유저가 특정 식당에 남긴 북마크 1건 조회 (해제할 때 대상을 찾기 위해 사용)
    Optional<BookMark> findByUserAndRestaurant(User user, Restaurant restaurant);

    // 특정 유저가 특정 식당을 이미 북마크했는지 여부만 빠르게 확인 (중복 등록 방지용)
    boolean existsByUserAndRestaurant(User user, Restaurant restaurant);

    // 특정 유저(userIdx)가 등록한 모든 북마크 목록 조회
    List<BookMark> findByUserUserIdx(int userIdx);
}
