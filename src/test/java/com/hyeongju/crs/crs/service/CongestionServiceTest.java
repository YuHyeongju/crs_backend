package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Congestion;
import com.hyeongju.crs.crs.domain.CongestionStatus;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.CongestionUpdateDto;
import com.hyeongju.crs.crs.dto.MyCongestionResponseDto;
import com.hyeongju.crs.crs.repository.CongestionRepository;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CongestionServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private CongestionRepository congestionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RewardService rewardService;

    @InjectMocks
    private CongestionService congestionService;

    private User user;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserIdx(1);

        restaurant = new Restaurant();
        restaurant.setRestIdx(10);
        restaurant.setKakaoId("kakao-1");
        restaurant.setRestName("맛있는집");
    }

    private Congestion congestion(CongestionStatus status, LocalDateTime at) {
        Congestion c = new Congestion();
        c.setCongStatus(status);
        c.setCongAt(at);
        c.setRestaurant(restaurant);
        c.setUser(user);
        return c;
    }

    // ===================== 혼잡도 제보 =====================

    @Test
    @DisplayName("restIdx 로 제보 - 쿨다운을 지났으면 저장 후 리워드 지급")
    void changeCongStatus_withRestIdx_grantsReward() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(1);
        dto.setRestIdx(10);
        dto.setCongStatus("BUSY");

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(congestionRepository.existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(
                eq(1), eq(10), any(LocalDateTime.class))).willReturn(false);

        congestionService.changeCongStatus(dto);

        ArgumentCaptor<Congestion> captor = ArgumentCaptor.forClass(Congestion.class);
        verify(congestionRepository).save(captor.capture());
        assertThat(captor.getValue().getCongStatus()).isEqualTo(CongestionStatus.BUSY);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getRestaurant()).isSameAs(restaurant);
        verify(rewardService).grantCongestionReward(user, restaurant);
    }

    @Test
    @DisplayName("30분 이내 같은 가게에 재제보하면 저장은 되지만 리워드는 지급되지 않는다")
    void changeCongStatus_withinCooldown_noReward() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(1);
        dto.setRestIdx(10);
        dto.setCongStatus("FREE");

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(congestionRepository.existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(
                eq(1), eq(10), any(LocalDateTime.class))).willReturn(true);

        congestionService.changeCongStatus(dto);

        verify(congestionRepository).save(any(Congestion.class));
        verify(rewardService, never()).grantCongestionReward(any(User.class), any(Restaurant.class));
    }

    @Test
    @DisplayName("restIdx 로 찾은 가게가 없으면 IllegalStateException")
    void changeCongStatus_restaurantNotFound() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(1);
        dto.setRestIdx(999);
        dto.setCongStatus("BUSY");

        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> congestionService.changeCongStatus(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Restaurant not found");
    }

    @Test
    @DisplayName("kakaoId 가 db- 접두어면 뒤의 숫자를 restIdx 로 해석한다")
    void changeCongStatus_dbPrefixedKakaoId() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(1);
        dto.setKakaoId("db-10");
        dto.setCongStatus("NORMAL");

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(congestionRepository.existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(
                anyInt(), anyInt(), any(LocalDateTime.class))).willReturn(false);

        congestionService.changeCongStatus(dto);

        verify(restaurantRepository).findByRestIdx(10);
        verify(congestionRepository).save(any(Congestion.class));
    }

    @Test
    @DisplayName("kakaoId 로 가게를 찾을 수 있으면 그대로 사용한다")
    void changeCongStatus_findsByKakaoId() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(1);
        dto.setKakaoId("kakao-1");
        dto.setCongStatus("VERY_BUSY");

        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(restaurant));
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(congestionRepository.existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(
                anyInt(), anyInt(), any(LocalDateTime.class))).willReturn(false);

        congestionService.changeCongStatus(dto);

        verify(restaurantRepository, never()).save(any(Restaurant.class));
        verify(congestionRepository).save(any(Congestion.class));
    }

    @Test
    @DisplayName("kakaoId/전화번호로도 못 찾으면 가게를 새로 생성한다")
    void changeCongStatus_createsRestaurantWhenMissing() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(1);
        dto.setKakaoId("kakao-new");
        dto.setRestName("새로운집");
        dto.setRestAddress("서울시 마포구");
        dto.setRestPhone("02-999-8888");
        dto.setCongStatus("FREE");

        given(restaurantRepository.findByKakaoId("kakao-new")).willReturn(Optional.empty());
        given(restaurantRepository.findByRestTel("02-999-8888")).willReturn(Optional.empty());
        given(restaurantRepository.save(any(Restaurant.class))).willAnswer(inv -> inv.getArgument(0));
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(congestionRepository.existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(
                anyInt(), anyInt(), any(LocalDateTime.class))).willReturn(false);

        congestionService.changeCongStatus(dto);

        ArgumentCaptor<Restaurant> captor = ArgumentCaptor.forClass(Restaurant.class);
        verify(restaurantRepository).save(captor.capture());
        assertThat(captor.getValue().getKakaoId()).isEqualTo("kakao-new");
        assertThat(captor.getValue().getRestName()).isEqualTo("새로운집");
        assertThat(captor.getValue().getRestAddress()).isEqualTo("서울시 마포구");
        assertThat(captor.getValue().getRestTel()).isEqualTo("02-999-8888");
    }

    @Test
    @DisplayName("유저를 찾을 수 없으면 IllegalStateException")
    void changeCongStatus_userNotFound() {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(99);
        dto.setRestIdx(10);
        dto.setCongStatus("BUSY");

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(userRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> congestionService.changeCongStatus(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("해당하는 유저가 없음");
        verify(congestionRepository, never()).save(any(Congestion.class));
    }

    // ===================== 혼잡도 조회 =====================

    @Test
    @DisplayName("kakaoId 로 현재 혼잡도 조회 - 마지막 제보 상태를 반환")
    void getCurrentcongestion_returnsLatest() {
        List<Congestion> history = new ArrayList<>();
        history.add(congestion(CongestionStatus.FREE, LocalDateTime.now().minusHours(2)));
        history.add(congestion(CongestionStatus.VERY_BUSY, LocalDateTime.now()));
        restaurant.setCongestions(history);

        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(restaurant));

        assertThat(congestionService.getCurrentcongestion("kakao-1")).isEqualTo("매우 혼잡");
    }

    @Test
    @DisplayName("제보 이력이 없으면 '혼잡도 이력 없음'")
    void getCurrentcongestion_noHistory() {
        restaurant.setCongestions(new ArrayList<>());
        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(restaurant));

        assertThat(congestionService.getCurrentcongestion("kakao-1"))
                .isEqualTo(CongestionStatus.NONE.getName());
    }

    @Test
    @DisplayName("가게 자체가 없으면 '혼잡도 이력 없음'")
    void getCurrentcongestion_restaurantNotFound() {
        given(restaurantRepository.findByKakaoId(anyString())).willReturn(Optional.empty());

        assertThat(congestionService.getCurrentcongestion("unknown"))
                .isEqualTo(CongestionStatus.NONE.getName());
    }

    @Test
    @DisplayName("restIdx 로 조회 시 congAt 이 가장 최근인 제보 상태를 반환")
    void getCurrentCongestionByRestIdx_returnsMostRecent() {
        List<Congestion> history = new ArrayList<>();
        history.add(congestion(CongestionStatus.FREE, LocalDateTime.now().minusMinutes(5)));
        history.add(congestion(CongestionStatus.BUSY, LocalDateTime.now().minusHours(3)));
        restaurant.setCongestions(history);

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThat(congestionService.getCurrentCongestionByRestIdx(10)).isEqualTo("여유");
    }

    @Test
    @DisplayName("restIdx 로 가게를 못 찾으면 '혼잡도 이력 없음'")
    void getCurrentCongestionByRestIdx_notFound() {
        given(restaurantRepository.findByRestIdx(anyInt())).willReturn(Optional.empty());

        assertThat(congestionService.getCurrentCongestionByRestIdx(999))
                .isEqualTo(CongestionStatus.NONE.getName());
    }

    @Test
    @DisplayName("여러 가게 혼잡도 일괄 조회 - 없는 가게는 NONE 으로 채워진다")
    void getAllCurrentCongestion() {
        List<Congestion> history = new ArrayList<>();
        history.add(congestion(CongestionStatus.NORMAL, LocalDateTime.now()));
        restaurant.setCongestions(history);

        given(restaurantRepository.findByKakaoIdIn(List.of("kakao-1", "kakao-missing")))
                .willReturn(List.of(restaurant));

        Map<String, String> result =
                congestionService.getAllCurrentCongestion(List.of("kakao-1", "kakao-missing"));

        assertThat(result).hasSize(2);
        assertThat(result.get("kakao-1")).isEqualTo("보통");
        assertThat(result.get("kakao-missing")).isEqualTo(CongestionStatus.NONE.getName());
    }

    @Test
    @DisplayName("kakaoId 목록이 null 이면 빈 맵 반환")
    void getAllCurrentCongestion_nullInput() {
        assertThat(congestionService.getAllCurrentCongestion(null)).isEmpty();
        verify(restaurantRepository, never()).findByKakaoIdIn(any());
    }

    @Test
    @DisplayName("kakaoId 목록이 비어있으면 빈 맵 반환")
    void getAllCurrentCongestion_emptyInput() {
        assertThat(congestionService.getAllCurrentCongestion(List.of())).isEmpty();
    }

    @Test
    @DisplayName("내 혼잡도 제보 이력 조회")
    void getMyCongestionHistory() {
        Congestion c = congestion(CongestionStatus.BUSY, LocalDateTime.now());
        c.setCongIdx(55);
        given(congestionRepository.findByUserUserIdxOrderByCongAtDesc(1)).willReturn(List.of(c));

        List<MyCongestionResponseDto> result = congestionService.getMyCongestionHistory(1);

        assertThat(result).hasSize(1);
        // 첫 필드명은 userIdx 이지만 실제로는 congIdx 가 담긴다
        assertThat(result.get(0).getUserIdx()).isEqualTo(55);
        assertThat(result.get(0).getRestName()).isEqualTo("맛있는집");
        assertThat(result.get(0).getStatus()).isEqualTo("혼잡");
        assertThat(result.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("제보 이력이 없으면 빈 목록")
    void getMyCongestionHistory_empty() {
        given(congestionRepository.findByUserUserIdxOrderByCongAtDesc(1)).willReturn(List.of());

        assertThat(congestionService.getMyCongestionHistory(1)).isEmpty();
    }
}
