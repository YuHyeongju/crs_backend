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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    private User merchant;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        // @Value 로 주입되는 필드는 단위 테스트에서 직접 세팅
        ReflectionTestUtils.setField(restaurantService, "uploadPath", "build/test-uploads/");
        ReflectionTestUtils.setField(restaurantService, "baseUrl", "http://localhost:8080");

        merchant = new User();
        merchant.setUserIdx(1);
        merchant.setName("사장님");

        restaurant = new Restaurant();
        restaurant.setRestIdx(10);
        restaurant.setKakaoId("kakao-1");
        restaurant.setRestName("맛있는집");
        restaurant.setRestAddress("서울시 강남구");
        restaurant.setRestTel("02-123-4567");
        restaurant.setLatitude(37.5);
        restaurant.setLongitude(127.0);
        restaurant.setUser(merchant);
    }

    // ===================== 단건 조회 =====================

    @Test
    @DisplayName("restIdx 로 식당 조회 성공")
    void getRestaurantByRestIdx_success() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThat(restaurantService.getRestaurantByRestIdx(10)).isSameAs(restaurant);
    }

    @Test
    @DisplayName("존재하지 않는 restIdx 조회 시 IllegalArgumentException")
    void getRestaurantByRestIdx_notFound() {
        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantByRestIdx(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가게를 찾을 수 없습니다");
    }

    // ===================== getOrCreateRestaurant =====================

    @Test
    @DisplayName("기존 식당의 비어있던 정보만 채워서 저장한다")
    void getOrCreateRestaurant_fillsBlankFields() {
        Restaurant existing = new Restaurant();
        existing.setKakaoId("kakao-1");
        existing.setRestName(null);
        existing.setRestAddress("  ");
        existing.setRestTel("02-111-1111"); // 이미 값이 있으므로 유지되어야 함

        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(existing));
        given(restaurantRepository.saveAndFlush(existing)).willReturn(existing);

        Restaurant result = restaurantService.getOrCreateRestaurant(
                "kakao-1", "새이름", "새주소", "02-999-9999");

        assertThat(result.getRestName()).isEqualTo("새이름");
        assertThat(result.getRestAddress()).isEqualTo("새주소");
        assertThat(result.getRestTel()).isEqualTo("02-111-1111");
        verify(restaurantRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("기존 식당 정보가 모두 채워져 있으면 저장하지 않는다")
    void getOrCreateRestaurant_noChangeNoSave() {
        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(restaurant));

        Restaurant result = restaurantService.getOrCreateRestaurant(
                "kakao-1", "다른이름", "다른주소", "02-999-9999");

        assertThat(result).isSameAs(restaurant);
        assertThat(result.getRestName()).isEqualTo("맛있는집");
        verify(restaurantRepository, never()).saveAndFlush(any(Restaurant.class));
    }

    @Test
    @DisplayName("기존 식당이 없으면 새로 생성한다")
    void getOrCreateRestaurant_createsNew() {
        given(restaurantRepository.findByKakaoId("kakao-new")).willReturn(Optional.empty());
        given(restaurantRepository.saveAndFlush(any(Restaurant.class))).willAnswer(inv -> inv.getArgument(0));

        Restaurant result = restaurantService.getOrCreateRestaurant(
                "kakao-new", "새로운집", "서울시 마포구", "02-777-7777");

        assertThat(result.getKakaoId()).isEqualTo("kakao-new");
        assertThat(result.getRestName()).isEqualTo("새로운집");
        assertThat(result.getRestAddress()).isEqualTo("서울시 마포구");
        assertThat(result.getRestTel()).isEqualTo("02-777-7777");
    }

    @Test
    @DisplayName("동시 저장으로 유니크 제약 위반이 나면 다시 조회해서 반환한다")
    void getOrCreateRestaurant_handlesConcurrentInsert() {
        given(restaurantRepository.findByKakaoId("kakao-1"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(restaurant));
        given(restaurantRepository.saveAndFlush(any(Restaurant.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        Restaurant result = restaurantService.getOrCreateRestaurant(
                "kakao-1", "맛있는집", "서울시 강남구", "02-123-4567");

        assertThat(result).isSameAs(restaurant);
    }

    // ===================== 상인 가게 등록 =====================

    private RestaurantRequestDto fullRequestDto() {
        RestaurantRequestDto dto = new RestaurantRequestDto();
        dto.setRestName("새가게");
        dto.setRestAddress("서울시 종로구");
        dto.setRestTel("02-555-5555");
        dto.setRestBusiHours("10:00 - 22:00");
        dto.setLatitude(37.1);
        dto.setLongitude(127.1);

        RestaurantRequestDto.FacilitiesDto facilities = new RestaurantRequestDto.FacilitiesDto();
        facilities.setWifi(true);
        facilities.setRestRoom(true);
        facilities.setParkingAvailable(false);
        facilities.setPackingPossible(true);
        facilities.setKakaoPay(true);
        facilities.setSamsungPay(false);
        facilities.setKiosk(true);
        dto.setFacilities(facilities);

        RestaurantRequestDto.MenuList menu = new RestaurantRequestDto.MenuList();
        menu.setMenuName("김치찌개");
        menu.setMenuPrice(9000);
        dto.setMenulist(List.of(menu));

        return dto;
    }

    @Test
    @DisplayName("상인 가게 등록 성공 - 승인 대기 상태로 메뉴/편의시설과 함께 저장된다")
    void registerRestaurantByMerchant_success() throws IOException {
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));
        given(restaurantRepository.save(any(Restaurant.class))).willAnswer(inv -> inv.getArgument(0));

        Restaurant result = restaurantService.registerRestaurantByMerchant(fullRequestDto(), 1, null);

        assertThat(result.getRestName()).isEqualTo("새가게");
        assertThat(result.getApprovalStatus()).isEqualTo("PENDING");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getUser()).isSameAs(merchant);
        assertThat(result.getLatitude()).isEqualTo(37.1);
        assertThat(result.getFacilities()).hasSize(1);
        assertThat(result.getFacilities().get(0).isWifi()).isTrue();
        assertThat(result.getFacilities().get(0).isParkingAvailable()).isFalse();
        assertThat(result.getMenuList()).hasSize(1);
        assertThat(result.getMenuList().get(0).getMenuName()).isEqualTo("김치찌개");
        assertThat(result.getMenuList().get(0).getMenuPrice()).isEqualTo(9000);
    }

    @Test
    @DisplayName("kakaoId 가 있으면 기존 식당을 찾아 내 가게로 등록한다")
    void registerRestaurantByMerchant_claimsExistingKakaoRestaurant() throws IOException {
        Restaurant kakaoOnly = new Restaurant();
        kakaoOnly.setKakaoId("kakao-1");
        kakaoOnly.setUser(null);

        RestaurantRequestDto dto = fullRequestDto();
        dto.setKakaoId("kakao-1");

        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));
        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(kakaoOnly));
        given(restaurantRepository.save(any(Restaurant.class))).willAnswer(inv -> inv.getArgument(0));

        Restaurant result = restaurantService.registerRestaurantByMerchant(dto, 1, null);

        assertThat(result).isSameAs(kakaoOnly);
        assertThat(result.getUser()).isSameAs(merchant);
    }

    @Test
    @DisplayName("이미 다른 상인이 등록한 가게면 IllegalStateException")
    void registerRestaurantByMerchant_alreadyClaimed() {
        User otherMerchant = new User();
        otherMerchant.setUserIdx(2);

        Restaurant claimed = new Restaurant();
        claimed.setKakaoId("kakao-1");
        claimed.setUser(otherMerchant);

        RestaurantRequestDto dto = fullRequestDto();
        dto.setKakaoId("kakao-1");

        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));
        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(claimed));

        assertThatThrownBy(() -> restaurantService.registerRestaurantByMerchant(dto, 1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 다른 사업자가 등록한 가게입니다.");
    }

    @Test
    @DisplayName("상인 정보를 찾을 수 없으면 RuntimeException")
    void registerRestaurantByMerchant_merchantNotFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.registerRestaurantByMerchant(fullRequestDto(), 99, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("상인 정보를 찾을 수 가 없습니다.");
    }

    // ===================== 목록/상세 =====================

    @Test
    @DisplayName("내 가게 목록 조회 - 평점은 소수점 첫째 자리로 반올림된다")
    void getMyRestaurants() {
        given(restaurantRepository.findByUserUserIdxAndApprovalStatus(1, "APPROVED"))
                .willReturn(List.of(restaurant));
        given(reviewRepository.findAverageRatingByRestaurantRestIdx(10)).willReturn(Optional.of(4.26));
        given(reviewRepository.countByRestaurantRestIdx(10)).willReturn(3);

        List<RestaurantResponseDto> result = restaurantService.getMyRestaurants(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestIdx()).isEqualTo(10);
        assertThat(result.get(0).getAverageRating()).isEqualTo(4.3);
        assertThat(result.get(0).getReviewCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("식당 상세 조회 성공 - 리뷰가 없으면 평점 0.0")
    void getRestaurantDetails_success() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(reviewRepository.findAverageRatingByRestaurantRestIdx(10)).willReturn(Optional.empty());
        given(reviewRepository.countByRestaurantRestIdx(10)).willReturn(0);

        RestaurantResponseDto dto = restaurantService.getRestaurantDetails(10);

        assertThat(dto.getRestName()).isEqualTo("맛있는집");
        assertThat(dto.getAverageRating()).isEqualTo(0.0);
        assertThat(dto.getReviewCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 식당 상세 조회 시 IllegalStateException")
    void getRestaurantDetails_notFound() {
        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantDetails(999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("해당 식당 정보를 찾을 수 없음");
    }

    @Test
    @DisplayName("kakaoId 목록 일괄 상세 조회 - 통계가 매핑된다")
    void getBulkDetailsByKakaoIds() {
        given(restaurantRepository.findByKakaoIdIn(List.of("kakao-1"))).willReturn(List.of(restaurant));
        given(reviewRepository.findRatingStatsByRestIdxIn(List.of(10)))
                .willReturn(List.<Object[]>of(new Object[]{10, 4.55, 4L}));

        Map<String, RestaurantResponseDto> result =
                restaurantService.getBulkDetailsByKakaoIds(List.of("kakao-1"));

        assertThat(result).containsKey("kakao-1");
        assertThat(result.get("kakao-1").getAverageRating()).isEqualTo(4.6);
        assertThat(result.get("kakao-1").getReviewCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("통계가 없는 식당은 평점 0.0 / 리뷰 0건으로 채워진다")
    void getBulkDetailsByKakaoIds_noStats() {
        given(restaurantRepository.findByKakaoIdIn(List.of("kakao-1"))).willReturn(List.of(restaurant));
        given(reviewRepository.findRatingStatsByRestIdxIn(List.of(10))).willReturn(List.of());

        Map<String, RestaurantResponseDto> result =
                restaurantService.getBulkDetailsByKakaoIds(List.of("kakao-1"));

        assertThat(result.get("kakao-1").getAverageRating()).isEqualTo(0.0);
        assertThat(result.get("kakao-1").getReviewCount()).isZero();
    }

    @Test
    @DisplayName("kakaoId 목록이 null 이거나 비어있으면 빈 맵을 반환한다")
    void getBulkDetailsByKakaoIds_emptyInput() {
        assertThat(restaurantService.getBulkDetailsByKakaoIds(null)).isEmpty();
        assertThat(restaurantService.getBulkDetailsByKakaoIds(List.of())).isEmpty();
        verify(restaurantRepository, never()).findByKakaoIdIn(any());
    }

    @Test
    @DisplayName("조회된 식당이 하나도 없으면 빈 맵을 반환한다")
    void getBulkDetailsByKakaoIds_noRestaurants() {
        given(restaurantRepository.findByKakaoIdIn(List.of("nope"))).willReturn(List.of());

        assertThat(restaurantService.getBulkDetailsByKakaoIds(List.of("nope"))).isEmpty();
    }

    @Test
    @DisplayName("kakaoId 로 상세 조회 성공 - 소유 상인 idx 포함")
    void getRestaurantDetailsByKakaoId_found() {
        given(restaurantRepository.findByKakaoId("kakao-1")).willReturn(Optional.of(restaurant));
        given(reviewRepository.findAverageRatingByRestaurantRestIdx(10)).willReturn(Optional.of(3.0));
        given(reviewRepository.countByRestaurantRestIdx(10)).willReturn(2);

        RestaurantResponseDto dto = restaurantService.getRestaurantDetailsByKakaoId("kakao-1");

        assertThat(dto.getRestIdx()).isEqualTo(10);
        assertThat(dto.getOwnerUserIdx()).isEqualTo(1);
        assertThat(dto.getAverageRating()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("DB 에 없는 kakaoId 는 빈 DTO 를 반환한다")
    void getRestaurantDetailsByKakaoId_notFound() {
        given(restaurantRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        RestaurantResponseDto dto = restaurantService.getRestaurantDetailsByKakaoId("unknown");

        assertThat(dto.getRestIdx()).isNull();
        assertThat(dto.getRestName()).isNull();
        assertThat(dto.getAverageRating()).isEqualTo(0.0);
        assertThat(dto.getReviewCount()).isZero();
        assertThat(dto.getOwnerUserIdx()).isNull();
    }

    // ===================== 수정 화면 / 수정 =====================

    @Test
    @DisplayName("가게 수정 폼 조회 성공 - 메뉴/편의시설이 DTO 로 변환된다")
    void getRestaurantForEdit_success() {
        RestaurantFacilities facilities = new RestaurantFacilities();
        facilities.setWifi(true);
        facilities.setKiosk(true);
        facilities.setRestaurant(restaurant);
        restaurant.getFacilities().add(facilities);

        RestaurantMenu menu = new RestaurantMenu();
        menu.setMenuName("김치찌개");
        menu.setMenuPrice(9000);
        menu.setMenuPict("pict.jpg");
        restaurant.getMenuList().add(menu);

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        RestaurantRequestDto dto = restaurantService.getRestaurantForEdit(10, 1);

        assertThat(dto.getRestName()).isEqualTo("맛있는집");
        assertThat(dto.getFacilities().isWifi()).isTrue();
        assertThat(dto.getFacilities().isKiosk()).isTrue();
        assertThat(dto.getMenulist()).hasSize(1);
        assertThat(dto.getMenulist().get(0).getMenuName()).isEqualTo("김치찌개");
        assertThat(dto.getMenulist().get(0).getMenuPict()).isEqualTo("pict.jpg");
    }

    @Test
    @DisplayName("본인 가게가 아니면 수정 폼 조회 시 SecurityException")
    void getRestaurantForEdit_notOwner() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.getRestaurantForEdit(10, 999))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인이 등록한 가게만 조회할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 가게 수정 폼 조회 시 IllegalStateException")
    void getRestaurantForEdit_notFound() {
        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantForEdit(999, 1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("가게 정보 수정 성공 - 메뉴는 통째로 교체된다")
    void updateRestaurantByMerchant_success() throws IOException {
        RestaurantMenu oldMenu = new RestaurantMenu();
        oldMenu.setMenuName("옛날메뉴");
        oldMenu.setMenuPrice(1000);
        restaurant.getMenuList().add(oldMenu);

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        Restaurant result = restaurantService.updateRestaurantByMerchant(10, 1, fullRequestDto(), null);

        assertThat(result.getRestName()).isEqualTo("새가게");
        assertThat(result.getRestBusiHours()).isEqualTo("10:00 - 22:00");
        assertThat(result.getMenuList()).hasSize(1);
        assertThat(result.getMenuList().get(0).getMenuName()).isEqualTo("김치찌개");
        assertThat(result.getFacilities()).hasSize(1);
        assertThat(result.getFacilities().get(0).isPackingPossible()).isTrue();
    }

    @Test
    @DisplayName("기존 편의시설이 있으면 새로 추가하지 않고 갱신한다")
    void updateRestaurantByMerchant_reusesExistingFacilities() throws IOException {
        RestaurantFacilities existing = new RestaurantFacilities();
        existing.setWifi(false);
        existing.setRestaurant(restaurant);
        restaurant.getFacilities().add(existing);

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        Restaurant result = restaurantService.updateRestaurantByMerchant(10, 1, fullRequestDto(), null);

        assertThat(result.getFacilities()).hasSize(1);
        assertThat(result.getFacilities().get(0)).isSameAs(existing);
        assertThat(existing.isWifi()).isTrue();
    }

    @Test
    @DisplayName("본인 가게가 아니면 수정 시 SecurityException")
    void updateRestaurantByMerchant_notOwner() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.updateRestaurantByMerchant(10, 999, fullRequestDto(), null))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인이 등록한 가게만 수정할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 가게 수정 시 IllegalStateException")
    void updateRestaurantByMerchant_notFound() {
        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.updateRestaurantByMerchant(999, 1, fullRequestDto(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수정할 식당 정보를 찾을 수 없음");
    }

    // ===================== 메뉴 =====================

    @Test
    @DisplayName("메뉴 조회 시 저장 파일명이 전체 URL 로 변환된다")
    void getMenusByRestIdx() {
        RestaurantMenu withPict = new RestaurantMenu();
        withPict.setMenuIdx(1);
        withPict.setMenuName("김치찌개");
        withPict.setMenuPrice(9000);
        withPict.setMenuPict("pict.jpg");

        RestaurantMenu withoutPict = new RestaurantMenu();
        withoutPict.setMenuIdx(2);
        withoutPict.setMenuName("된장찌개");
        withoutPict.setMenuPrice(8000);

        restaurant.getMenuList().add(withPict);
        restaurant.getMenuList().add(withoutPict);

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        List<MenuResponseDto> result = restaurantService.getMenusByRestIdx(10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getImageUrl()).isEqualTo("http://localhost:8080/uploads/pict.jpg");
        assertThat(result.get(1).getImageUrl()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 가게의 메뉴 조회 시 IllegalStateException")
    void getMenusByRestIdx_notFound() {
        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getMenusByRestIdx(999))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===================== 삭제 =====================

    @Test
    @DisplayName("본인 가게 삭제 성공")
    void deleteRestaurant_success() {
        RestaurantMenu menu = new RestaurantMenu();
        menu.setMenuPict("not-exists.jpg"); // 실제 파일이 없어도 예외 없이 넘어가야 함
        restaurant.getMenuList().add(menu);

        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant(10, 1);

        verify(restaurantRepository).delete(restaurant);
    }

    @Test
    @DisplayName("본인 가게가 아니면 삭제 시 SecurityException")
    void deleteRestaurant_notOwner() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> restaurantService.deleteRestaurant(10, 999))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인이 등록한 가게만 삭제할 수 있습니다.");
        verify(restaurantRepository, never()).delete(any(Restaurant.class));
    }

    @Test
    @DisplayName("존재하지 않는 가게 삭제 시 IllegalStateException")
    void deleteRestaurant_notFound() {
        given(restaurantRepository.findByRestIdx(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.deleteRestaurant(999, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제할 식당을 찾을 수 없습니다.");
    }

    // ===================== 지도 핀 =====================

    @Test
    @DisplayName("승인된 상인 가게 핀 목록 조회")
    void getApprovedMerchantPins() {
        given(restaurantRepository
                .findByUserIsNotNullAndApprovalStatusAndLatitudeIsNotNullAndLongitudeIsNotNull("APPROVED"))
                .willReturn(List.of(restaurant));
        given(reviewRepository.findAverageRatingByRestaurantRestIdx(10)).willReturn(Optional.of(4.44));
        given(reviewRepository.countByRestaurantRestIdx(10)).willReturn(9);

        List<RestaurantPinDto> result = restaurantService.getApprovedMerchantPins();

        assertThat(result).hasSize(1);
        RestaurantPinDto pin = result.get(0);
        assertThat(pin.getRestIdx()).isEqualTo(10);
        assertThat(pin.getLatitude()).isEqualTo(37.5);
        assertThat(pin.getLongitude()).isEqualTo(127.0);
        assertThat(pin.getKakaoId()).isEqualTo("kakao-1");
        assertThat(pin.getAverageRating()).isEqualTo(4.4);
        assertThat(pin.getReviewCount()).isEqualTo(9);
        assertThat(pin.getOwnerUserIdx()).isEqualTo(1);
    }

    @Test
    @DisplayName("restIdx 로 지도 핀 단건 조회 - 소유자가 없으면 ownerUserIdx 는 null")
    void getRestaurantPinByRestIdx() {
        restaurant.setUser(null);
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(reviewRepository.findAverageRatingByRestaurantRestIdx(10)).willReturn(Optional.empty());
        given(reviewRepository.countByRestaurantRestIdx(10)).willReturn(0);

        RestaurantPinDto pin = restaurantService.getRestaurantPinByRestIdx(10);

        assertThat(pin.getOwnerUserIdx()).isNull();
        assertThat(pin.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("존재하지 않는 가게의 지도 핀 조회 시 IllegalStateException")
    void getRestaurantPinByRestIdx_notFound() {
        given(restaurantRepository.findByRestIdx(anyInt())).willReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantPinByRestIdx(999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Restaurant not found");
    }

    @Test
    @DisplayName("리뷰 통계 조회는 restIdx 기준으로 호출된다")
    void reviewStatsAreQueriedByRestIdx() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(reviewRepository.findAverageRatingByRestaurantRestIdx(anyInt())).willReturn(Optional.of(5.0));
        given(reviewRepository.countByRestaurantRestIdx(anyInt())).willReturn(1);

        restaurantService.getRestaurantDetails(10);

        verify(reviewRepository).findAverageRatingByRestaurantRestIdx(10);
        verify(reviewRepository).countByRestaurantRestIdx(10);
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }
}
