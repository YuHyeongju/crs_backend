package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    private Restaurant restaurant;


}
