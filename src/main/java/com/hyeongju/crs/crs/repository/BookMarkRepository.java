package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.BookMark;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookMarkRepository extends JpaRepository<BookMark, Integer> {

    Optional<BookMark> findByUserAndRestaurant(User user, Restaurant restaurant);

    boolean existsByUserAndRestaurant(User user, Restaurant restaurant);

    List<BookMark> findByUserUserIdx(int userIdx);
}
