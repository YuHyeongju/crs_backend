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
public class UserDetailsResponseDto {
    // 관리자 화면의 "유저 상세보기" 응답 DTO (목록용보다 필드가 더 많음)
    private int userIdx;
    private String id;
    private String userType;          // RoleName (USER/MERCHANT/ADMIN)
    private String email;
    private String name;
    private String phNum;
    private String gender;
    private String businessNum;       // 상인일 때만 값 있음
    private String adminNum;          // 관리자일 때만 값 있음
    private LocalDateTime createTime;
    private String status;
    private long congestionCount;
    private long reviewCount;

    public UserDetailsResponseDto(User user, long congestionCount, long reviewCount) {
        // User 엔티티 -> DTO 필드 매핑 (건수는 서비스 계층에서 별도 조회해 넘겨받음)
        this.userIdx = user.getUserIdx();
        this.id = user.getId();
        this.userType = user.getRole().getRoleName().name();
        this.email = user.getEmail();
        this.name = user.getName();
        this.phNum = user.getPhNum();
        this.gender = user.getGender();
        this.businessNum = user.getBusinessNum();
        this.adminNum = user.getAdminNum();
        this.createTime = user.getCreateTime();
        this.status = user.getStatus();
        this.congestionCount = congestionCount;
        this.reviewCount = reviewCount;
    }
}
