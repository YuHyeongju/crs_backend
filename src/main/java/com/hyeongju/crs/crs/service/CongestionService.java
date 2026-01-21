package com.hyeongju.crs.crs.service;
import com.hyeongju.crs.crs.domain.Congestion;
import com.hyeongju.crs.crs.domain.CongestionStatus;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.CongestionUpdateDto;
import com.hyeongju.crs.crs.repository.CongestionRepository;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor

public class CongestionService {
    private final RestaurantRepository restaurantRepository;
    private final CongestionRepository congestionRepository;

    @Transactional
    public void changeCongLev(CongestionUpdateDto dto){

        System.out.println("전달받은 DTO 내용: " + dto.toString());
        System.out.println("조회하려는 식당 IDX:" + dto.getRestIdx());
        if (dto.getCongLevIdx() == null) {
            throw new IllegalArgumentException("혼잡도 레벨 ID(congLevIdx)가 전달되지 않았습니다.");
        }

        CongestionStatus newStatus = CongestionStatus.convertIdx(dto.getCongLevIdx());

        Restaurant restaurant = restaurantRepository.findByKakaoId(dto.getRestIdx())
                .orElseGet(()-> {
                    System.out.println("새로운 식당(KakaoId: " + dto.getRestIdx() + ")을 등록 합니다.");
                    Restaurant newRestaurant = new Restaurant();
                    newRestaurant.setKakaoId(dto.getRestIdx());
                    return restaurantRepository.save(newRestaurant);
                });

        Congestion congestion = new Congestion();
        congestion.setRestaurant(restaurant);
        congestion.setCongStatus(newStatus);

        congestionRepository.save(congestion);

        System.out.println("변환된 혼잡도 상태:" + newStatus.getName());
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String getCurrentcongestion(String kakaoId){
        return restaurantRepository.findByKakaoId(kakaoId)
                .map(r ->{
                        // 혼잡도 이력 조회
                        List< Congestion > history = r.getCongestions();
                        // 혼잡도 이력 없음
                        if(history == null || history.isEmpty()){
                            return "혼잡도 이력 없음";
                        }
                        // 마지막 혼잡도 이력 가져와서 보여줌
                        Congestion lastCong = history.get(history.size() -1);
                        return lastCong.getCongStatus().getName();

                }).orElse("식당 정보 없음");


    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Map<String, String> getAllCurrentCongestion(List<String> kakaoIds){
        Map<String, String> resultMap = new HashMap<>();

        if(kakaoIds == null || kakaoIds.isEmpty()){
            return resultMap;
        }
        for(String id : kakaoIds){
            String status = getCurrentcongestion(id);
            resultMap.put(id, status);
        }
        return resultMap;
    }

}
