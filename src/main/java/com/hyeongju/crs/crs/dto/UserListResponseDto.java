package com.hyeongju.crs.crs.dto;

import com.hyeongju.crs.crs.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponseDto {
    // 관리자 화면의 "유저 목록" 응답 DTO (요약 정보만 포함)
    private int userIdx;
    private String id;
    private String userType;
    private String email;
    private LocalDateTime createTime;
    private String status;
    private long congestionCount;
    private long reviewCount;

    public UserListResponseDto(User user, long congestionCount, long reviewCount) {
        // 건수는 엔티티 관계 탐색이 아닌 서비스 계층의 별도 COUNT 쿼리로 구해 넘겨받음 (N+1 방지)
        this.userIdx = user.getUserIdx();
        this.id = user.getId();
        this.userType = user.getRole().getRoleName().name();
        this.email = user.getEmail();
        this.createTime = user.getCreateTime();
        this.status = user.getStatus();
        this.congestionCount = congestionCount;
        this.reviewCount = reviewCount;
    }
}
