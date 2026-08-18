package com.hyeongju.crs.crs.repository;


import com.hyeongju.crs.crs.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {


    // 카카오 지도 API의 식당 식별자로 조회 (이미 등록된 식당인지 중복 체크용)
    Optional <Restaurant> findByKakaoId(String kakaoId);

    Optional <Restaurant> findByRestIdx(int restIdx);

    // 승인 상태(PENDING/APPROVED 등)로 목록 조회 (관리자 승인 대기 목록 등에 사용)
    List <Restaurant> findByApprovalStatus(String approvalStatus);

    // 승인 상태 + 데이터 상태(TEMP 등) 둘 다 일치하는 목록 조회
    List <Restaurant> findByApprovalStatusAndStatus(String approvalStatus, String status);

    List<Restaurant> findByKakaoIdIn(List<String> kakaoIds); // 여러개 kakaoId 한 번에 조회

    // 특정 상인(userIdx)이 소유하고 특정 승인 상태인 식당 목록
    List <Restaurant> findByUserUserIdxAndApprovalStatus(int userIdx, String approvalStatus);

    // 상인이 등록했고(user가 null이 아님) 승인됐고 위도/경도가 채워진 식당만 조회 (지도에 핀을 찍을 대상)
    List<Restaurant> findByUserIsNotNullAndApprovalStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(String approvalStatus);

    // 전화번호로 단건 조회 (중복 등록 여부 확인용)
    Optional<Restaurant> findByRestTel(String restTel);
}
