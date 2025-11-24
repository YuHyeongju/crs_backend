package com.hyeongju.crs.crs.domain;

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
    @JoinColumn(name = "USER_IDX",referencedColumnName = "USER_IDX",nullable = false)
    private int userIdx;

    @Column(name = "TOTAL_REWARD_VALUE",nullable = false)
    private int totalRewardValue;

    @Column(name = "REWARD_AT",nullable = false)
    private LocalDateTime rewardAt;

    @Column(name = "REWARD_REASON",length = 500)
    private String rewardReason;
}
