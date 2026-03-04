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

    private FacilitiesDto facilities;

    @Getter @Setter
    public static class MenuList{
        private String menuName;
        private int    menuPrice;
        private String menuPict;
    }

    @Getter @Setter
    public static class FacilitiesDto{
        private boolean wifi;
        private boolean restRoom;
        private boolean parkingAvailable;
        private boolean packingPossible;
        private boolean kakaoPay;
        private boolean samsungPay;
        private boolean kiosk;
    }
}
