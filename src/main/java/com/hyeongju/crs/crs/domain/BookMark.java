package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "bookmark")
@NoArgsConstructor
public class BookMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookMarkIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",referencedColumnName = "USER_IDX",nullable = false,unique = true)
    private int userIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",referencedColumnName = "REST_IDX",nullable = false,unique = true)
    private int restIdx;

    @Column(name = "BOOKMARKED_AT",nullable = false)
    private LocalDateTime bookMarkedAt;
}
