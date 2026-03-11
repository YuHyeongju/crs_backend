package com.hyeongju.crs.crs.repository;


import com.hyeongju.crs.crs.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByRestaurant_RestIdxOrderByReviewAtDesc(int restIdx);
}
