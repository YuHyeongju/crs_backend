package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Table(name = "congestion")
@NoArgsConstructor
public class Congestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONG_IDX")
    private int congIdx;

    @Enumerated(EnumType.STRING) // DB에 혼잡도 상태를 문자열로 저장
    @Column(name = "CONG_Status")
    private CongestionStatus congStatus;

    @CreatedDate
    @Column(name = "CONG_AT",nullable = false,updatable = false)
    private LocalDateTime congAt;

    @Column(name = "CONG_SHOW",nullable = false)
    private boolean congShow;

    @Column(name = "CONG_UPDATE_AT")
    private LocalDateTime congUpdateAt;

    @Column(name = "CONG_HIDDEN_AT")
    private LocalDateTime congHiddenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    private Restaurant restaurant;


}
