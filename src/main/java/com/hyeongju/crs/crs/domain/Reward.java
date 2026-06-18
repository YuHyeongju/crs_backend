package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward")
@Getter
@Setter
@NoArgsConstructor

public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int rewardIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",nullable = false)
    @JsonIgnore
    private User user;

    // 어떤 가게 제보로 적립됐는지 (카카오 자동생성 가게 등도 있어 nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX")
    @JsonIgnore
    private Restaurant restaurant;

    // 적립 이벤트 1건당 지급된 포인트 (유저 잔액은 이 값들의 SUM으로 계산)
    @Column(name = "TOTAL_REWARD_VALUE",nullable = false)
    private int totalRewardValue;

    @Column(name = "REWARD_AT",nullable = false)
    private LocalDateTime rewardAt;

    @Column(name = "REWARD_REASON",length = 500)
    private String rewardReason;
}
