package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ReviewRequestDto {
    private int restIdx;

    @NotBlank(message = "리뷰 내용은 필수 입력 값입니다.")
    private String content;

    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private int rating;
    private String userName;
    private int userIdx;
}
