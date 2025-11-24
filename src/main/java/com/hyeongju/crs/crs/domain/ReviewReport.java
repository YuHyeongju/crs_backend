package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "review_report")
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int  reportIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTER_USER_IDX",referencedColumnName = "USER_IDX",nullable = false)
    private int reporterUserIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTED_USER_IDX",referencedColumnName = "USER_IDX",nullable = false)
    private int reportedUserIdx;

    @Column(name = "REPORTED_AT",nullable = false)
    private LocalDateTime reportAT;

    @Column(name = "REASON",length = 200,nullable = false)
    private String reason;


}
