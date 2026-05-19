package com.hyeongju.crs.crs.repository;


import com.hyeongju.crs.crs.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByRestaurant_RestIdxOrderByReviewAtDesc(int restIdx);

    long countByUserUserIdx(int userIdx);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.restIdx = :restIdx")
    Optional<Double> findAverageRatingByRestaurantRestIdx(@Param("restIdx") int restIdx);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.restIdx = :restIdx")
    Integer countByRestaurantRestIdx(@Param("restIdx") int restIdx);

    // [restIdx, avgRating, reviewCount] — 여러 식당의 평점/리뷰수를 한 쿼리로
    @Query("SELECT r.restaurant.restIdx, AVG(r.rating), COUNT(r) " +
            "FROM Review r WHERE r.restaurant.restIdx IN :restIdxes " +
            "GROUP BY r.restaurant.restIdx")
    List<Object[]> findRatingStatsByRestIdxIn(@Param("restIdxes") List<Integer> restIdxes);
}
