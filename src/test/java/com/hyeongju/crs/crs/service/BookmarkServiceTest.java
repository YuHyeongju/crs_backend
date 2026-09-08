package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.BookMark;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BookMarkDto;
import com.hyeongju.crs.crs.repository.BookMarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookMarkRepository bookMarkRepository;
    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private BookmarkService bookmarkService;

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
        restaurant.setRestAddress("서울시 강남구");
        restaurant.setRestTel("02-123-4567");
    }

    @Test
    @DisplayName("restIdx 로 북마크 토글 - 기존 북마크가 없으면 add")
    void toggleBookMark_addByRestIdx() {
        BookMarkDto dto = new BookMarkDto();
        dto.setRestIdx(10);

        given(restaurantService.getRestaurantByRestIdx(10)).willReturn(restaurant);
        given(bookMarkRepository.findByUserAndRestaurant(user, restaurant)).willReturn(Optional.empty());

        String result = bookmarkService.toggleBookMark(user, dto);

        assertThat(result).isEqualTo("add");
        ArgumentCaptor<BookMark> captor = ArgumentCaptor.forClass(BookMark.class);
        verify(bookMarkRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getRestaurant()).isSameAs(restaurant);
        assertThat(captor.getValue().getBookMarkedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 북마크되어 있으면 delete 를 반환하고 삭제한다")
    void toggleBookMark_delete() {
        BookMarkDto dto = new BookMarkDto();
        dto.setRestIdx(10);

        BookMark existing = new BookMark();
        given(restaurantService.getRestaurantByRestIdx(10)).willReturn(restaurant);
        given(bookMarkRepository.findByUserAndRestaurant(user, restaurant)).willReturn(Optional.of(existing));

        String result = bookmarkService.toggleBookMark(user, dto);

        assertThat(result).isEqualTo("delete");
        verify(bookMarkRepository).delete(existing);
        verify(bookMarkRepository, never()).save(any(BookMark.class));
    }

    @Test
    @DisplayName("restIdx 가 없고 kakaoId 만 있으면 식당을 조회/생성해서 북마크한다")
    void toggleBookMark_byKakaoId() {
        BookMarkDto dto = new BookMarkDto();
        dto.setKakaoId("kakao-1");
        dto.setRestName("맛있는집");
        dto.setRestAddress("서울시 강남구");
        dto.setRestTel("02-123-4567");

        given(restaurantService.getOrCreateRestaurant("kakao-1", "맛있는집", "서울시 강남구", "02-123-4567"))
                .willReturn(restaurant);
        given(bookMarkRepository.findByUserAndRestaurant(user, restaurant)).willReturn(Optional.empty());

        String result = bookmarkService.toggleBookMark(user, dto);

        assertThat(result).isEqualTo("add");
        verify(restaurantService).getOrCreateRestaurant("kakao-1", "맛있는집", "서울시 강남구", "02-123-4567");
    }

    @Test
    @DisplayName("restIdx / kakaoId 둘 다 없으면 IllegalArgumentException")
    void toggleBookMark_noIdentifier() {
        BookMarkDto dto = new BookMarkDto();

        assertThatThrownBy(() -> bookmarkService.toggleBookMark(user, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("restIdx 또는 kakaoId 중 하나는 필수입니다.");
    }

    @Test
    @DisplayName("북마크 kakaoId 목록 조회 - kakaoId 가 없는 식당은 db-{restIdx} 형태로 대체된다")
    void getBookmarkKakaoIds() {
        Restaurant noKakao = new Restaurant();
        noKakao.setRestIdx(77);
        noKakao.setKakaoId(null);

        BookMark bm1 = new BookMark();
        bm1.setRestaurant(restaurant);
        BookMark bm2 = new BookMark();
        bm2.setRestaurant(noKakao);

        given(bookMarkRepository.findByUserUserIdx(1)).willReturn(List.of(bm1, bm2));

        List<String> result = bookmarkService.getBookmarkKakaoIds(1);

        assertThat(result).containsExactly("kakao-1", "db-77");
    }

    @Test
    @DisplayName("북마크가 없으면 빈 목록을 반환한다")
    void getBookmarkKakaoIds_empty() {
        given(bookMarkRepository.findByUserUserIdx(1)).willReturn(List.of());

        assertThat(bookmarkService.getBookmarkKakaoIds(1)).isEmpty();
    }

    @Test
    @DisplayName("마이페이지용 북마크 상세 목록 조회")
    void getBookmarkListForMypage() {
        BookMark bm = new BookMark();
        bm.setRestaurant(restaurant);
        given(bookMarkRepository.findByUserUserIdx(1)).willReturn(List.of(bm));

        List<BookMarkDto> result = bookmarkService.getBookmarkListForMypage(1);

        assertThat(result).hasSize(1);
        BookMarkDto dto = result.get(0);
        assertThat(dto.getUserIdx()).isEqualTo(1);
        assertThat(dto.getRestIdx()).isEqualTo(10);
        assertThat(dto.getKakaoId()).isEqualTo("kakao-1");
        assertThat(dto.getRestName()).isEqualTo("맛있는집");
        assertThat(dto.getRestAddress()).isEqualTo("서울시 강남구");
        assertThat(dto.getRestTel()).isEqualTo("02-123-4567");
    }
}
