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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class CongestionService {
    private final RestaurantRepository restaurantRepository;
    private final CongestionRepository congestionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void changeCongStatus(CongestionUpdateDto dto){
        Restaurant restaurant = restaurantRepository.findByKakaoId(dto.getKakaoId())
                .orElseGet(()-> {
                            Restaurant newRest = new Restaurant();
                            newRest.setKakaoId(dto.getKakaoId());
                            newRest.setRestName(dto.getRestName());
                            newRest.setRestAddress(dto.getRestAddress());
                            newRest.setRestTel(dto.getRestPhone());
                            return restaurantRepository.save(newRest);
                        });

        User user = userRepository.findById(dto.getUserIdx())
                .orElseThrow(() -> new IllegalStateException("해당하는 유저가 없음"));

        Congestion congestion = new Congestion();
        congestion.setRestaurant(restaurant);
        congestion.setUser(user);
        congestion.setCongStatus(CongestionStatus.valueOf(dto.getCongStatus()));

        congestionRepository.save(congestion);

    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String getCurrentcongestion(String kakaoId){
        return restaurantRepository.findByKakaoId(kakaoId)
                .map(r ->{
                        // 혼잡도 이력 조회
                        List< Congestion > history = r.getCongestions();
                        // 혼잡도 이력 없음
                        if(history == null || history.isEmpty()){
                            return CongestionStatus.NONE.getName();
                        }
                        // 마지막 혼잡도 이력 가져와서 보여줌
                        Congestion lastCong = history.get(history.size() -1);
                        return lastCong.getCongStatus().getName();

                }).orElse(CongestionStatus.NONE.getName());


    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, String> getAllCurrentCongestion(List<String> kakaoIds){
        Map<String, String> resultMap = new HashMap<>();

        if(kakaoIds == null || kakaoIds.isEmpty()){
            return resultMap;
        }

        // Fetch all restaurants in a single query
        List<Restaurant> restaurants = restaurantRepository.findByKakaoIdIn(kakaoIds);

        // Create a map for quick lookup of restaurants by kakaoId
        Map<String, Restaurant> restaurantMap = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getKakaoId, restaurant -> restaurant));

        // Process the fetched restaurants
        for (String kakaoId : kakaoIds) {
            Restaurant restaurant = restaurantMap.get(kakaoId);
            String status = CongestionStatus.NONE.getName(); // Default to NONE

            if (restaurant != null) {
                List<Congestion> history = restaurant.getCongestions(); // @BatchSize will optimize this

                if (history != null && !history.isEmpty()) {
                    // Sort history by CongAt to get the latest congestion
                    history.sort((c1, c2) -> c2.getCongAt().compareTo(c1.getCongAt()));
                    Congestion lastCong = history.get(0);
                    status = lastCong.getCongStatus().getName();
                }
            }
            resultMap.put(kakaoId, status);
        }

        return resultMap;
    }

    public List<MyCongestionResponseDto>  getMyCongestionHistory(int userIdx){
        List<Congestion> history = congestionRepository.findByUserUserIdxOrderByCongAtDesc(userIdx);

        return history.stream().map(c -> new MyCongestionResponseDto(
                c.getCongIdx(),
                c.getRestaurant().getRestName(),
                c.getCongStatus().getName(),
                c.getCongAt()
        )).collect(Collectors.toList());
    }

}
