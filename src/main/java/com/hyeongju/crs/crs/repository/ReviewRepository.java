package com.hyeongju.crs.crs.repository;


import com.hyeongju.crs.crs.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    @Query("SELECT r FROM Review r WHERE r.restaurant.restIdx = :restIdx AND r.status = 'ACTIVE' ORDER BY r.reviewAt DESC")
    List<Review> findByRestaurant_RestIdxOrderByReviewAtDesc(@Param("restIdx") int restIdx);

    @Query(value = "SELECT r FROM Review r WHERE r.user.userIdx = :userIdx AND r.status = 'ACTIVE' ORDER BY r.reviewAt DESC",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.user.userIdx = :userIdx AND r.status = 'ACTIVE'")
    Page<Review> findActiveByUserIdxOrderByReviewAtDesc(@Param("userIdx") int userIdx, Pageable pageable);

    long countByUserUserIdx(int userIdx);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.restIdx = :restIdx AND r.status = 'ACTIVE'")
    Optional<Double> findAverageRatingByRestaurantRestIdx(@Param("restIdx") int restIdx);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.restIdx = :restIdx AND r.status = 'ACTIVE'")
    Integer countByRestaurantRestIdx(@Param("restIdx") int restIdx);

    // [restIdx, avgRating, reviewCount] — 여러 식당의 평점/리뷰수를 한 쿼리로
    @Query("SELECT r.restaurant.restIdx, AVG(r.rating), COUNT(r) " +
            "FROM Review r WHERE r.restaurant.restIdx IN :restIdxes AND r.status = 'ACTIVE' " +
            "GROUP BY r.restaurant.restIdx")
    List<Object[]> findRatingStatsByRestIdxIn(@Param("restIdxes") List<Integer> restIdxes);
}
