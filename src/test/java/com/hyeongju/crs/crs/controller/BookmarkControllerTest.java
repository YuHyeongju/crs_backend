package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BookMarkDto;
import com.hyeongju.crs.crs.repository.UserRepository;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.BookmarkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static com.hyeongju.crs.crs.controller.TestAuth.authentication;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookmarkController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookmarkService bookMarkService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private User user() {
        User user = new User();
        user.setUserIdx(1);
        user.setName("홍길동");
        return user;
    }

    @Test
    @DisplayName("POST /api/bookmarks/toggle - 북마크 추가 시 add 반환")
    void toggleBookMark_add() throws Exception {
        User user = user();
        given(userRepository.findById(1)).willReturn(Optional.of(user));
        given(bookMarkService.toggleBookMark(any(User.class), any(BookMarkDto.class))).willReturn("add");

        BookMarkDto dto = new BookMarkDto();
        dto.setRestIdx(10);

        mockMvc.perform(post("/api/bookmarks/toggle")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("add"));
    }

    @Test
    @DisplayName("POST /api/bookmarks/toggle - 북마크 해제 시 delete 반환")
    void toggleBookMark_delete() throws Exception {
        given(userRepository.findById(1)).willReturn(Optional.of(user()));
        given(bookMarkService.toggleBookMark(any(User.class), any(BookMarkDto.class))).willReturn("delete");

        BookMarkDto dto = new BookMarkDto();
        dto.setKakaoId("kakao-1");

        mockMvc.perform(post("/api/bookmarks/toggle")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("delete"));
    }

    @Test
    @DisplayName("POST /api/bookmarks/toggle - 인증 정보가 없으면 401")
    void toggleBookMark_unauthorized() throws Exception {
        BookMarkDto dto = new BookMarkDto();
        dto.setRestIdx(10);

        mockMvc.perform(post("/api/bookmarks/toggle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));

        verify(bookMarkService, never()).toggleBookMark(any(User.class), any(BookMarkDto.class));
    }

    @Test
    @DisplayName("POST /api/bookmarks/toggle - 유저를 찾을 수 없으면 400 (IllegalStateException)")
    void toggleBookMark_userNotFound() throws Exception {
        given(userRepository.findById(1)).willReturn(Optional.empty());

        BookMarkDto dto = new BookMarkDto();
        dto.setRestIdx(10);

        mockMvc.perform(post("/api/bookmarks/toggle")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("해당 하는 유저 없음"));
    }

    @Test
    @DisplayName("GET /api/bookmarks/my-bookmark-list/{userIdx} - kakaoId 목록 반환")
    void getUserBookMarkIds_success() throws Exception {
        given(bookMarkService.getBookmarkKakaoIds(1)).willReturn(List.of("kakao-1", "db-77"));

        mockMvc.perform(get("/api/bookmarks/my-bookmark-list/1").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("kakao-1"))
                .andExpect(jsonPath("$[1]").value("db-77"));
    }

    @Test
    @DisplayName("GET /api/bookmarks/my-bookmark-list/{userIdx} - 인증 정보가 없으면 401")
    void getUserBookMarkIds_unauthorized() throws Exception {
        mockMvc.perform(get("/api/bookmarks/my-bookmark-list/1"))
                .andExpect(status().isUnauthorized());

        verify(bookMarkService, never()).getBookmarkKakaoIds(anyInt());
    }

    @Test
    @DisplayName("GET /api/bookmarks/my-bookmark-list/{userIdx} - 경로변수가 아닌 토큰의 userIdx 로 조회한다")
    void getUserBookMarkIds_usesAuthenticatedUserIdx() throws Exception {
        given(bookMarkService.getBookmarkKakaoIds(1)).willReturn(List.of("kakao-1"));

        mockMvc.perform(get("/api/bookmarks/my-bookmark-list/999").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk());

        verify(bookMarkService).getBookmarkKakaoIds(1);
        verify(bookMarkService, never()).getBookmarkKakaoIds(999);
    }

    @Test
    @DisplayName("GET /api/bookmarks/details - 북마크 상세 목록 반환")
    void getMyBookmarkDetails_success() throws Exception {
        BookMarkDto dto = new BookMarkDto();
        dto.setUserIdx(1);
        dto.setRestIdx(10);
        dto.setKakaoId("kakao-1");
        dto.setRestName("맛있는집");
        dto.setRestAddress("서울시 강남구");
        dto.setRestTel("02-123-4567");

        given(bookMarkService.getBookmarkListForMypage(1)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/bookmarks/details").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restIdx").value(10))
                .andExpect(jsonPath("$[0].restName").value("맛있는집"))
                .andExpect(jsonPath("$[0].kakaoId").value("kakao-1"));
    }

    @Test
    @DisplayName("GET /api/bookmarks/details - 인증 정보가 없으면 401")
    void getMyBookmarkDetails_unauthorized() throws Exception {
        mockMvc.perform(get("/api/bookmarks/details"))
                .andExpect(status().isUnauthorized());
    }
}
