package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.Reward;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.repository.RewardRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;

    // 혼잡도 제보 1건당 지급 포인트 (1단계 MVP — 고정값, 추후 가게별 배율/관리자 설정으로 확장 예정)
    private static final int REPORT_REWARD_POINT = 100;

    // 혼잡도 제보 보상 적립 (적립 장부에 1건 추가)
    @Transactional
    public void grantCongestionReward(User user, Restaurant restaurant) {
        Reward reward = new Reward();
        reward.setUser(user);
        reward.setRestaurant(restaurant);
        reward.setTotalRewardValue(REPORT_REWARD_POINT);
        reward.setRewardAt(LocalDateTime.now());
        reward.setRewardReason("혼잡도 제보 보상 - " + restaurant.getRestName());
        rewardRepository.save(reward);
    }

    // 유저 보유 포인트 잔액 = 적립 내역 합계
    public int getBalance(int userIdx) {
        return rewardRepository.sumRewardValueByUserIdx(userIdx);
    }
}
