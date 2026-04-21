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
    private int userIdx;
    private String id;
    private String userType; // RoleName
    private String email;
    private LocalDateTime createTime;
    private String status;
    private long congestionCount;
    private long reviewCount;

    public UserListResponseDto(User user, long congestionCount, long reviewCount) {
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
