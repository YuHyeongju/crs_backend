package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 리뷰 엔티티. 삭제/차단 시에도 물리적으로 지우지 않고 status를 바꾸는 소프트 삭제 방식 사용
@Entity
@Getter
@Setter
@Table(name = "review")
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int reviewIdx;

    @Column(name = "RATING",nullable = false)
    private int rating;

    @Column(name = "CONTENT",length = 2000,nullable = false)
    private String content;

    @Column(name = "REVIEW_AT",nullable = false)
    private LocalDateTime reviewAt;

    @Column(name = "REVIEW_UPDATE_AT")
    private LocalDateTime reviewUpdateAt;

    @Column(name = "REVIEW_DELETE_AT")
    private LocalDateTime reviewDeleteAt;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, BLOCKED, DELETED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    @JsonIgnore
    private Restaurant restaurant;


}
