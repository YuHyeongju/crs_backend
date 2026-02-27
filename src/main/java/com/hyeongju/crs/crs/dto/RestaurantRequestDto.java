package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class RestaurantRequestDto {
    private String kakaoId;
    private String restName;
    private String restTel;
    private String restAddress;
    private String restBusiHours;

    private List<MenuList> menulist;

    @Getter @Setter
    public static class MenuList{
        private String menuName;
        private int    menuPrice;
    }
}
