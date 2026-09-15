package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.RestaurantFacilities;
import com.hyeongju.crs.crs.domain.RestaurantMenu;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MenuResponseDto;
import com.hyeongju.crs.crs.dto.RestaurantPinDto;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    // 식당 등록/수정/조회/삭제, 메뉴·편의시설 관리, 지도 핀 데이터 제공 등 식당 관련 핵심 로직을 모아둔 서비스

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Value("${app.upload.dir}")
    private String uploadPath; // 메뉴 이미지가 저장되는 서버 로컬 경로(설정파일에서 주입)

    @Value("${app.base-url}")
    private String baseUrl; // 이미지 URL을 만들 때 앞에 붙일 서버 기본 주소(설정파일에서 주입)



    public Restaurant getRestaurantByRestIdx(int restIdx) {
        // restIdx로 식당 엔티티 단건 조회
        return restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + restIdx));
    }

    @Transactional
    public Restaurant getOrCreateRestaurant(String kakaoId, String restName, String restAddress, String restTel) {
        // kakaoId로 식당을 찾고, 있으면 비어있던 정보를 채워 갱신, 없으면 새로 생성
        // (동시에 같은 kakaoId로 여러 요청이 들어와 저장이 충돌하는 경우까지 대비함)

        return restaurantRepository.findByKakaoId(kakaoId)
                .map(existing -> {
                    boolean changed = false;
                    if (isBlank(existing.getRestName()) && !isBlank(restName)) {
                        existing.setRestName(restName);
                        changed = true;
                    }
                    if (isBlank(existing.getRestAddress()) && !isBlank(restAddress)) {
                        existing.setRestAddress(restAddress);
                        changed = true;
                    }
                    if (isBlank(existing.getRestTel()) && !isBlank(restTel)) {
                        existing.setRestTel(restTel);
                        changed = true;
                    }
                    // 기존 값이 비어있던 필드만 채워넣고, 실제로 바뀐 게 있을 때만 즉시 flush해서 저장
                    return changed ? restaurantRepository.saveAndFlush(existing) : existing;
                })
                .orElseGet(() -> {
                    try {
                        Restaurant newRestaurant = new Restaurant();
                        newRestaurant.setKakaoId(kakaoId);
                        newRestaurant.setRestName(restName);
                        newRestaurant.setRestAddress(restAddress);
                        newRestaurant.setRestTel(restTel);
                        return restaurantRepository.saveAndFlush(newRestaurant);
                    } catch (DataIntegrityViolationException e) {
                        // 동시에 같은 kakaoId로 저장을 시도해 유니크 제약 위반이 나면, 그사이 다른 요청이 만든 레코드를 다시 조회해서 반환
                        return restaurantRepository.findByKakaoId(kakaoId)
                                .orElseThrow(() -> new RuntimeException("가게 정보 등록 중 동시성 오류 발생"));
                    }
                });
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Transactional
    public Restaurant registerRestaurantByMerchant(RestaurantRequestDto dto, int userIdx,
                                                   List<MultipartFile> menuImages) throws IOException {
        // 상인이 자기 가게를 등록(또는 카카오 자동생성 식당을 "내 가게로 등록")하는 로직

        User merchant = userRepository.findByUserIdx(userIdx)
                .orElseThrow(() -> new RuntimeException("상인 정보를 찾을 수 가 없습니다."));

        String merchantName = merchant.getName();


        Restaurant restaurant = isBlank(dto.getKakaoId())
                ? new Restaurant() // kakaoId가 없으면 완전히 새로운 식당(카카오에 없는 가게)
                : restaurantRepository.findByKakaoId(dto.getKakaoId())
                    .orElseGet(() -> {
                        // kakaoId는 있는데 DB에는 아직 없으면(카카오 지도에서 처음 등록하는 경우) 새로 생성
                        Restaurant newRestaurant = new Restaurant();
                        newRestaurant.setKakaoId(dto.getKakaoId());
                        return newRestaurant;
                    });

        // 이미 다른 상인이 등록(claim)한 가게면 거절 — 본인이 이미 가진 가게는 재등록(수정) 허용
        if (restaurant.getUser() != null && restaurant.getUser().getUserIdx() != userIdx) {
            throw new IllegalStateException("이미 다른 사업자가 등록한 가게입니다.");
        }

        restaurant.setRestName(dto.getRestName());
        restaurant.setRestAddress(dto.getRestAddress());
        restaurant.setRestTel(dto.getRestTel());
        restaurant.setRestBusiHours(dto.getRestBusiHours());

        if (dto.getLatitude() != null) restaurant.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) restaurant.setLongitude(dto.getLongitude());

        restaurant.setStatus("ACTIVE");
        restaurant.setApprovalStatus("PENDING"); // 등록 즉시 노출되지 않고 관리자 승인을 거쳐야 함
        restaurant.setUser(merchant);

        if (dto.getFacilities() != null) {
            // 편의시설 정보를 요청 DTO에서 엔티티로 옮겨 담음
            RestaurantFacilities facilities = new RestaurantFacilities();
            RestaurantRequestDto.FacilitiesDto fDto = dto.getFacilities();

            facilities.setWifi(fDto.isWifi());
            facilities.setRestRoom(fDto.isRestRoom());
            facilities.setParkingAvailable(fDto.isParkingAvailable());
            facilities.setPackingPossible(fDto.isPackingPossible());
            facilities.setKakaoPay(fDto.isKakaoPay());
            facilities.setSamsungPay(fDto.isSamsungPay());
            facilities.setKiosk(fDto.isKiosk());

            facilities.setRestaurant(restaurant);
            restaurant.getFacilities().add(facilities); // cascade=ALL이라 식당 저장 시 편의시설도 함께 저장됨
        }

        if (dto.getMenulist() != null) {

            int index = 0;
            for (RestaurantRequestDto.MenuList menuDto : dto.getMenulist()) {
                RestaurantMenu menu = new RestaurantMenu();

                log.debug("메뉴 이름: {}, 가격: {}", menuDto.getMenuName(), menuDto.getMenuPrice());

                menu.setMenuName(menuDto.getMenuName());
                menu.setMenuPrice(menuDto.getMenuPrice());

                if (menuImages != null && index < menuImages.size()) {
                    // 요청으로 넘어온 이미지 파일 리스트를 메뉴 순서와 그대로 매칭시켜 저장
                    MultipartFile imageFile = menuImages.get(index);
                    if (!imageFile.isEmpty()) {
                        String savedName = saveImage(imageFile,merchantName);
                        menu.setMenuPict(savedName);
                    }
                }

                menu.setRestaurant(restaurant);

                restaurant.getMenuList().add(menu);

                index++;
            }
        }

        return restaurantRepository.save(restaurant);
    }

    private String saveImage(MultipartFile file, String userName) throws IOException {
        // 업로드된 이미지 파일을 서버 로컬 디스크에 "타임스탬프_UUID_유저이름" 형태의 파일명으로 저장
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs(); // 업로드 폴더가 없으면 생성
            if (!created) {
                throw new IOException("폴더 생성 실패" + uploadPath);
            }
        }

        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());

        // 타임스탬프만으로는 같은 초에 동시 업로드되면 파일명이 겹쳐 덮어써지므로, UUID를 추가로 덧붙여 유일성을 보장한다
        String saveName = timeStamp + "_" + UUID.randomUUID() + "_" + userName;

        File target = new File(uploadPath + saveName);
        file.transferTo(target); // 실제 파일 저장

        return saveName;
    }

    public List<RestaurantResponseDto> getMyRestaurants(int userIdx) {
        // 상인 본인이 소유한, 승인 완료된 가게 목록 (평점/리뷰수 통계 포함)
        List<Restaurant> restaurants = restaurantRepository.findByUserUserIdxAndApprovalStatus(
                userIdx,"APPROVED");

        return restaurants.stream().map(restaurant -> {
            RestaurantResponseDto dto = new RestaurantResponseDto();
            dto.setRestIdx(restaurant.getRestIdx());
            dto.setRestName(restaurant.getRestName());
            dto.setRestAddress(restaurant.getRestAddress());

            Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                                                .orElse(0.0);
            Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

            dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0);
            dto.setReviewCount(reviewCount);

            return dto;
        }).collect(Collectors.toList());
        // 주의: 식당 개수만큼 리뷰 통계 쿼리가 반복 호출됨(N+1) — 대량 데이터일 경우 getBulkDetailsByKakaoIds처럼 집계 쿼리로 개선 여지가 있음
    }

    public RestaurantResponseDto getRestaurantDetails(int restIdx) {
        // 식당 1건의 상세 정보(이름/주소 + 평점/리뷰수) 조회

        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("해당 식당 정보를 찾을 수 없음: " + restIdx));

        RestaurantResponseDto dto = new RestaurantResponseDto();
        dto.setRestIdx(restaurant.getRestIdx());
        dto.setRestName(restaurant.getRestName());
        dto.setRestAddress(restaurant.getRestAddress());

        Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                                            .orElse(0.0);
        Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

        dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0);
        dto.setReviewCount(reviewCount);

        return dto;
    }

    // 여러 카카오ID의 평점/리뷰수를 한 번에 — 지도 핀 로딩 최적화용
    public Map<String, RestaurantResponseDto> getBulkDetailsByKakaoIds(List<String> kakaoIds) {
        Map<String, RestaurantResponseDto> result = new HashMap<>();
        if (kakaoIds == null || kakaoIds.isEmpty()) return result;

        List<Restaurant> restaurants = restaurantRepository.findByKakaoIdIn(kakaoIds);
        if (restaurants.isEmpty()) return result;

        List<Integer> restIdxes = restaurants.stream()
                .map(Restaurant::getRestIdx)
                .collect(Collectors.toList());

        // restIdx -> [avg, count]
        Map<Integer, double[]> statsMap = new HashMap<>();
        for (Object[] row : reviewRepository.findRatingStatsByRestIdxIn(restIdxes)) {
            Integer restIdx = (Integer) row[0];
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long count = ((Number) row[2]).longValue();
            statsMap.put(restIdx, new double[]{avg, count});
        }

        for (Restaurant r : restaurants) {
            double[] stats = statsMap.getOrDefault(r.getRestIdx(), new double[]{0.0, 0});
            RestaurantResponseDto dto = new RestaurantResponseDto();
            dto.setRestIdx(r.getRestIdx());
            dto.setRestName(r.getRestName());
            dto.setRestAddress(r.getRestAddress());
            dto.setAverageRating(Math.round(stats[0] * 10.0) / 10.0);
            dto.setReviewCount((int) stats[1]);
            result.put(r.getKakaoId(), dto); // 카카오ID를 키로 사용해 프론트에서 바로 매칭 가능하게 함
        }
        return result;
    }

    @Transactional
    public RestaurantResponseDto getRestaurantDetailsByKakaoId(String kakaoId) {
        // 카카오ID로 식당 상세 조회 (DB에 없으면 빈 DTO 반환)
        return restaurantRepository.findByKakaoId(kakaoId)
                .map(restaurant -> {
                    RestaurantResponseDto dto = new RestaurantResponseDto();
                    dto.setRestIdx(restaurant.getRestIdx());
                    dto.setRestName(restaurant.getRestName());
                    dto.setRestAddress(restaurant.getRestAddress());

                    Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                                                        .orElse(0.0);
                    Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

                    dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0);
                    dto.setReviewCount(reviewCount);
                    // 가게를 등록한 상인 식별자 — 미등록(카카오 자동생성) 식당이면 null
                    dto.setOwnerUserIdx(restaurant.getUser() != null ? restaurant.getUser().getUserIdx() : null);
                    return dto;
                })
                // DB 미등록 카카오ID: 빈 DTO 반환 (지도에서 처음 보는 식당 대응)
                .orElseGet(() -> new RestaurantResponseDto(null, null, null, 0.0, 0, null));
    }



    @Transactional
    public RestaurantRequestDto getRestaurantForEdit(int restIdx, int userIdx) {
        // 상인이 자기 가게 정보를 "수정" 화면에서 불러올 때, 엔티티를 요청 DTO 형태로 변환해서 돌려줌
        // (수정 폼에 기존 값을 채워 넣기 위함)

        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("해당 식당 정보를 찾을 수 없음: " + restIdx));
        if (restaurant.getUser() == null || restaurant.getUser().getUserIdx() != userIdx) {
            throw new SecurityException("본인이 등록한 가게만 조회할 수 있습니다.");
        }


        RestaurantRequestDto dto = new RestaurantRequestDto();
        dto.setRestName(restaurant.getRestName());
        dto.setRestAddress(restaurant.getRestAddress());
        dto.setRestTel(restaurant.getRestTel());
        dto.setRestBusiHours(restaurant.getRestBusiHours());


        if (!restaurant.getFacilities().isEmpty()) {
            RestaurantFacilities facilities = restaurant.getFacilities().get(0); // 편의시설은 사실상 1건만 존재
            RestaurantRequestDto.FacilitiesDto fDto = new RestaurantRequestDto.FacilitiesDto();
            fDto.setWifi(facilities.isWifi());
            fDto.setRestRoom(facilities.isRestRoom());
            fDto.setParkingAvailable(facilities.isParkingAvailable());
            fDto.setPackingPossible(facilities.isPackingPossible());
            fDto.setKakaoPay(facilities.isKakaoPay());
            fDto.setSamsungPay(facilities.isSamsungPay());
            fDto.setKiosk(facilities.isKiosk());
            dto.setFacilities(fDto);
        }


        List<RestaurantRequestDto.MenuList> menuListDtos = new ArrayList<>();
        for (RestaurantMenu menu : restaurant.getMenuList()) {
            RestaurantRequestDto.MenuList mDto = new RestaurantRequestDto.MenuList();
            mDto.setMenuName(menu.getMenuName());
            mDto.setMenuPrice(menu.getMenuPrice());
            mDto.setMenuPict(menu.getMenuPict()); // 기존 저장된 이미지 파일명
            menuListDtos.add(mDto);
        }
        dto.setMenulist(menuListDtos);

        return dto;
    }

    @Transactional
    public Restaurant updateRestaurantByMerchant(int restIdx, int userIdx, RestaurantRequestDto dto,
                                                 List<MultipartFile> menuImages) throws IOException {
        // 식당 정보 수정: 기본 정보 갱신 + 편의시설 갱신(없으면 신규 생성) + 메뉴는 통째로 비우고 다시 채움
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("수정할 식당 정보를 찾을 수 없음"));
        if (restaurant.getUser() == null || restaurant.getUser().getUserIdx() != userIdx) {
            throw new SecurityException("본인이 등록한 가게만 수정할 수 있습니다.");
        }
        String merchantName = restaurant.getUser().getName();

        restaurant.setRestName(dto.getRestName());
        restaurant.setRestAddress(dto.getRestAddress());
        restaurant.setRestTel(dto.getRestTel());
        restaurant.setRestBusiHours(dto.getRestBusiHours());

        if (dto.getFacilities() != null) {
            RestaurantFacilities facilities = restaurant.getFacilities().isEmpty() ? new RestaurantFacilities() :
                    restaurant.getFacilities().get(0); // 기존 편의시설이 있으면 재사용, 없으면 새로 생성

            RestaurantRequestDto.FacilitiesDto fDto = dto.getFacilities();
            facilities.setWifi(fDto.isWifi());
            facilities.setRestRoom(fDto.isRestRoom());
            facilities.setParkingAvailable(fDto.isParkingAvailable());
            facilities.setPackingPossible(fDto.isPackingPossible());
            facilities.setKakaoPay(fDto.isKakaoPay());
            facilities.setSamsungPay(fDto.isSamsungPay());
            facilities.setKiosk(fDto.isKiosk());

            if (restaurant.getFacilities().isEmpty()) {
                facilities.setRestaurant(restaurant);
                restaurant.getFacilities().add(facilities); // 새로 만든 경우에만 리스트에 추가(기존 것을 재사용했을 땐 이미 리스트 안에 있음)
            }
        }
        if (dto.getMenulist() != null) {
            restaurant.getMenuList().clear();
            // orphanRemoval=true라서 리스트를 비우면 기존 메뉴들이 DB에서도 삭제된 뒤, 아래에서 새로 채운 메뉴들이 저장됨

            int index = 0;
            for (RestaurantRequestDto.MenuList menuDto : dto.getMenulist()) {
                RestaurantMenu menu = new RestaurantMenu();
                menu.setMenuName(menuDto.getMenuName());
                menu.setMenuPrice(menuDto.getMenuPrice());

                if (menuImages != null && index < menuImages.size()) {
                    MultipartFile imageFile = menuImages.get(index);
                    if (!imageFile.isEmpty()) {
                        String saveName = saveImage(imageFile, merchantName);
                        menu.setMenuPict(saveName);
                    }
                }
                menu.setRestaurant(restaurant);
                restaurant.getMenuList().add(menu);
                index++;
            }
        }
        return restaurant;
    }


    public List<MenuResponseDto> getMenusByRestIdx(int restIdx) {
        // 특정 식당의 메뉴 목록 조회 (저장된 이미지 파일명을 완전한 URL로 변환)
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("해당 식당 정보를 찾을 수 없음: " + restIdx));
        return restaurant.getMenuList().stream()
                .map(menu -> new MenuResponseDto(
                        menu.getMenuIdx(),
                        menu.getMenuName(),
                        menu.getMenuPrice(),
                        menu.getMenuPict() != null ? baseUrl + "/uploads/" + menu.getMenuPict() : null
                        // 저장된 파일명 앞에 서버 기본 주소를 붙여 프론트가 바로 쓸 수 있는 완전한 URL로 변환
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRestaurant(int restIdx, int userIdx){
        // 식당 삭제 시, DB 행뿐 아니라 서버에 저장된 메뉴 이미지 실제 파일까지 함께 정리
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new IllegalStateException("삭제할 식당을 찾을 수 없습니다."));
        if (restaurant.getUser() == null || restaurant.getUser().getUserIdx() != userIdx) {
            throw new SecurityException("본인이 등록한 가게만 삭제할 수 있습니다.");
        }

        if(restaurant.getMenuList() != null){
            for(RestaurantMenu menu : restaurant.getMenuList()){
                if(menu.getMenuPict() != null && !menu.getMenuPict().isEmpty()){
                    deleteActualFile(menu.getMenuPict());
                }
            }
        }
        restaurantRepository.delete(restaurant); // cascade=ALL이라 메뉴/편의시설/북마크/리뷰/혼잡도 등 연관 데이터도 함께 삭제됨
    }

    public List<RestaurantPinDto> getApprovedMerchantPins() {
        // 지도에 표시할 "상인이 등록하고 승인된" 식당 핀 목록
        return restaurantRepository
                .findByUserIsNotNullAndApprovalStatusAndLatitudeIsNotNullAndLongitudeIsNotNull("APPROVED")
                .stream()
                .map(this::toRestaurantPinDto)
                .collect(Collectors.toList());
    }

    public RestaurantPinDto getRestaurantPinByRestIdx(int restIdx) {
        // 지도 핀 1건 조회
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("Restaurant not found: " + restIdx));

        return toRestaurantPinDto(restaurant);
    }

    private RestaurantPinDto toRestaurantPinDto(Restaurant restaurant) {
        // Restaurant 엔티티 -> 지도 핀 DTO 매핑 (평점/리뷰수 포함)
        Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                .orElse(0.0);
        Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

        return new RestaurantPinDto(
                restaurant.getRestIdx(),
                restaurant.getRestName(),
                restaurant.getRestAddress(),
                restaurant.getRestTel(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getKakaoId(),
                Math.round(averageRating * 10.0) / 10.0,
                reviewCount,
                restaurant.getUser() != null ? restaurant.getUser().getUserIdx() : null
        );
    }

    private void deleteActualFile(String fileName){
        // 업로드 디렉터리에서 실제 이미지 파일을 삭제(존재할 때만 시도)
        File file = new File(uploadPath + fileName);
        if(file.exists()){
            if(file.delete()){
                log.debug("파일 삭제 완료: {}", fileName);
            }else{
                log.warn("파일 삭제 실패: {}", fileName);
            }
        }
    }
}
