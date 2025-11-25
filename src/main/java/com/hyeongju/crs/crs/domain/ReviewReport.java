package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "review_report")
@NoArgsConstructor
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int  reportIdx;

    @Column(name = "REPORTED_AT",nullable = false)
    private LocalDateTime reportAt;

    @Column(name = "REASON",length = 200,nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTER_USER_IDX",nullable = false)
    // 여기서는 컬럼의 DB상의 실제 이름 작성
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTED_REVIEW_IDX",nullable = false)
    private Review reportedReview;
}
