package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.Reward;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private RewardService rewardService;

    private User user;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserIdx(1);

        restaurant = new Restaurant();
        restaurant.setRestIdx(10);
        restaurant.setRestName("맛있는집");
    }

    @Test
    @DisplayName("혼잡도 제보 보상은 +100 포인트로 적립된다")
    void grantCongestionReward() {
        rewardService.grantCongestionReward(user, restaurant);

        ArgumentCaptor<Reward> captor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(captor.capture());
        Reward reward = captor.getValue();

        assertThat(reward.getUser()).isSameAs(user);
        assertThat(reward.getRestaurant()).isSameAs(restaurant);
        assertThat(reward.getTotalRewardValue()).isEqualTo(100);
        assertThat(reward.getRewardAt()).isNotNull();
        assertThat(reward.getRewardReason()).isEqualTo("혼잡도 제보 보상 - 맛있는집");
    }

    @Test
    @DisplayName("포인트 차감은 음수 값으로 장부에 기록된다")
    void spendPoints() {
        rewardService.spendPoints(user, restaurant, 300, "쿠폰 교환 - 아메리카노");

        ArgumentCaptor<Reward> captor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepository).save(captor.capture());
        Reward reward = captor.getValue();

        assertThat(reward.getTotalRewardValue()).isEqualTo(-300);
        assertThat(reward.getRewardReason()).isEqualTo("쿠폰 교환 - 아메리카노");
        assertThat(reward.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("잔액 조회는 적립/차감 합계를 그대로 반환한다")
    void getBalance() {
        given(rewardRepository.sumRewardValueByUserIdx(1)).willReturn(700);

        assertThat(rewardService.getBalance(1)).isEqualTo(700);
    }

    @Test
    @DisplayName("적립 내역이 없으면 잔액은 0")
    void getBalance_zero() {
        given(rewardRepository.sumRewardValueByUserIdx(2)).willReturn(0);

        assertThat(rewardService.getBalance(2)).isZero();
    }
}
