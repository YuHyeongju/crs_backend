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

    // 특정 식당(restIdx)의 ACTIVE 상태 리뷰만 최신순으로 조회
    @Query("SELECT r FROM Review r WHERE r.restaurant.restIdx = :restIdx AND r.status = 'ACTIVE' ORDER BY r.reviewAt DESC")
    List<Review> findByRestaurant_RestIdxOrderByReviewAtDesc(@Param("restIdx") int restIdx);

    // 특정 유저(userIdx)가 쓴 ACTIVE 리뷰를 최신순으로 페이징 조회
    @Query(value = "SELECT r FROM Review r WHERE r.user.userIdx = :userIdx AND r.status = 'ACTIVE' ORDER BY r.reviewAt DESC",
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.user.userIdx = :userIdx AND r.status = 'ACTIVE'")
    Page<Review> findActiveByUserIdxOrderByReviewAtDesc(@Param("userIdx") int userIdx, Pageable pageable);

    // 특정 유저가 작성한 리뷰 총 개수
    long countByUserUserIdx(int userIdx);

    // 특정 식당의 ACTIVE 리뷰 평균 별점. 리뷰가 하나도 없으면 결과가 없을 수 있어 Optional로 감쌈
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.restIdx = :restIdx AND r.status = 'ACTIVE'")
    Optional<Double> findAverageRatingByRestaurantRestIdx(@Param("restIdx") int restIdx);

    // 특정 식당의 ACTIVE 리뷰 개수
    @Query("SELECT COUNT(r) FROM Review r WHERE r.restaurant.restIdx = :restIdx AND r.status = 'ACTIVE'")
    Integer countByRestaurantRestIdx(@Param("restIdx") int restIdx);

    // [restIdx, avgRating, reviewCount] — 식당마다 따로 쿼리하지 않고 한 번에 통계를 가져오는 집계 쿼리
    @Query("SELECT r.restaurant.restIdx, AVG(r.rating), COUNT(r) " +
            "FROM Review r WHERE r.restaurant.restIdx IN :restIdxes AND r.status = 'ACTIVE' " +
            "GROUP BY r.restaurant.restIdx")
    List<Object[]> findRatingStatsByRestIdxIn(@Param("restIdxes") List<Integer> restIdxes);
}
