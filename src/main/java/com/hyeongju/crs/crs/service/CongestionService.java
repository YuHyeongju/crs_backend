package com.hyeongju.crs.crs.service;
import com.hyeongju.crs.crs.domain.Congestion;
import com.hyeongju.crs.crs.domain.CongestionStatus;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.CongestionUpdateDto;
import com.hyeongju.crs.crs.dto.MyCongestionResponseDto;
import com.hyeongju.crs.crs.repository.CongestionRepository;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.hyeongju.crs.crs.domain.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class CongestionService {
    // 혼잡도 제보 등록/조회 + 제보 리워드 지급을 담당하는 서비스
    private final RestaurantRepository restaurantRepository;
    private final CongestionRepository congestionRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    // 혼잡도 제보 리워드 정책 (1단계 MVP) — 같은 가게는 30분에 한 번만 적립 인정
    private static final long REPORT_COOLDOWN_MINUTES = 30;

    @Transactional
    public void changeCongStatus(CongestionUpdateDto dto){
        // 혼잡도 제보 등록. 식당이 DB에 없으면(카카오에서 막 검색한 식당) 자동으로 생성까지 처리
        Restaurant restaurant = findRestaurantForCongestion(dto);

        User user = userRepository.findById(dto.getUserIdx())
                .orElseThrow(() -> new IllegalStateException("해당하는 유저가 없음"));

        // 적립 여부 판단은 '이번 제보 저장 전'에 확인해야 직전 제보 이력만 보게 됨
        LocalDateTime cooldownStart = LocalDateTime.now().minusMinutes(REPORT_COOLDOWN_MINUTES);
        boolean recentlyReported = congestionRepository
                .existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(
                        user.getUserIdx(), restaurant.getRestIdx(), cooldownStart);

        Congestion congestion = new Congestion();
        congestion.setRestaurant(restaurant);
        congestion.setUser(user);
        congestion.setCongStatus(CongestionStatus.valueOf(dto.getCongStatus()));

        congestionRepository.save(congestion);

        // 제보 자체는 항상 저장하고, 30분 쿨다운을 통과한 경우에만 포인트 지급 (어뷰징 방지)
        if (!recentlyReported) {
            rewardService.grantCongestionReward(user, restaurant);
        }
    }

    private Restaurant findRestaurantForCongestion(CongestionUpdateDto dto) {
        // 제보 대상 식당을 찾는 우선순위: ① restIdx로 직접 조회 ② "db-"로 시작하는 kakaoId(프론트에서 DB pk를 감싼 값) 파싱
        // ③ 진짜 kakaoId나 전화번호로 조회 ④ 그래도 없으면 새 식당을 만들어서 저장
        if (dto.getRestIdx() != null) {
            return restaurantRepository.findByRestIdx(dto.getRestIdx())
                    .orElseThrow(() -> new IllegalStateException("Restaurant not found: " + dto.getRestIdx()));
        }

        if (dto.getKakaoId() != null && dto.getKakaoId().startsWith("db-")) {
            // 프론트에서 "db-12" 형태로 restIdx를 kakaoId 자리에 실어 보내는 경우를 처리
            int restIdx = Integer.parseInt(dto.getKakaoId().substring(3));
            return restaurantRepository.findByRestIdx(restIdx)
                    .orElseThrow(() -> new IllegalStateException("Restaurant not found: " + restIdx));
        }

        return restaurantRepository.findByKakaoId(dto.getKakaoId())
                .or(() -> restaurantRepository.findByRestTel(dto.getRestPhone()))
                .orElseGet(()-> {
                            Restaurant newRest = new Restaurant();
                            newRest.setKakaoId(dto.getKakaoId());
                            newRest.setRestName(dto.getRestName());
                            newRest.setRestAddress(dto.getRestAddress());
                            newRest.setRestTel(dto.getRestPhone());
                            return restaurantRepository.save(newRest);
                        });
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true) // 조회 전용이라 변경 감지 비용 없음
    public String getCurrentcongestion(String kakaoId){
        // 카카오ID 기준 최신 혼잡도 조회 (구버전 API — restIdx 기준인 getCurrentCongestionByRestIdx와 로직이 중복됨)
        return restaurantRepository.findByKakaoId(kakaoId)
                .map(r ->{
                        List< Congestion > history = r.getCongestions();
                        if(history == null || history.isEmpty()){
                            return CongestionStatus.NONE.getName();
                        }
                        Congestion lastCong = history.get(history.size() -1);
                        return lastCong.getCongStatus().getName();

                }).orElse(CongestionStatus.NONE.getName());


    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String getCurrentCongestionByRestIdx(int restIdx){
        // restIdx 기준 최신 혼잡도 조회
        return restaurantRepository.findByRestIdx(restIdx)
                .map(this::getLatestCongestionName)
                .orElse(CongestionStatus.NONE.getName());
    }

    private String getLatestCongestionName(Restaurant restaurant) {
        List<Congestion> history = restaurant.getCongestions();
        if(history == null || history.isEmpty()){
            return CongestionStatus.NONE.getName();
        }
        history.sort((c1, c2) -> c2.getCongAt().compareTo(c1.getCongAt())); // 최신 시각이 앞에 오도록 내림차순 정렬
        return history.get(0).getCongStatus().getName();
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, String> getAllCurrentCongestion(List<String> kakaoIds){
        // 지도에 여러 식당 핀을 한 번에 표시할 때, kakaoId 목록을 받아 각각의 최신 혼잡도를 한 번에 반환
        Map<String, String> resultMap = new HashMap<>();

        if(kakaoIds == null || kakaoIds.isEmpty()){
            return resultMap;
        }

        List<Restaurant> restaurants = restaurantRepository.findByKakaoIdIn(kakaoIds); // 한 번의 IN 쿼리로 조회(N+1 방지)

        Map<String, Restaurant> restaurantMap = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getKakaoId, restaurant -> restaurant));

        for (String kakaoId : kakaoIds) {
            Restaurant restaurant = restaurantMap.get(kakaoId);
            String status = CongestionStatus.NONE.getName();

            if (restaurant != null) {
                List<Congestion> history = restaurant.getCongestions(); // @BatchSize로 조회 최적화됨

                if (history != null && !history.isEmpty()) {
                    history.sort((c1, c2) -> c2.getCongAt().compareTo(c1.getCongAt()));
                    Congestion lastCong = history.get(0);
                    status = lastCong.getCongStatus().getName();
                }
            }
            resultMap.put(kakaoId, status); // 요청받은 kakaoId 순서 그대로 채움(식당이 없으면 NONE)
        }

        return resultMap;
    }

    public List<MyCongestionResponseDto>  getMyCongestionHistory(int userIdx){
        // 마이페이지 "내가 제보한 혼잡도 이력" 조회
        List<Congestion> history = congestionRepository.findByUserUserIdxOrderByCongAtDesc(userIdx);

        return history.stream().map(c -> new MyCongestionResponseDto(
                // 주의: MyCongestionResponseDto의 첫 필드명은 userIdx이지만 실제로는 congIdx(제보 번호)가 들어감
                c.getCongIdx(),
                c.getRestaurant().getRestName(),
                c.getCongStatus().getName(),
                c.getCongAt()
        )).collect(Collectors.toList());
    }

}
