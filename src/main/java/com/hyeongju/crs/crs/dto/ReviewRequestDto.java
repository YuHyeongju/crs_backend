package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ReviewRequestDto {
    private int restIdx;
    private String content;
    private int rating;
    private String userName;
    private int userIdx;
}
