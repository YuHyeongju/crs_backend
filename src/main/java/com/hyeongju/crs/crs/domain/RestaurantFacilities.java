package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "restaurant_facilities")
@NoArgsConstructor
public class RestaurantFacilities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="FACILITIES_IDX")
    private int facilitiesIdx;

    @Column(name="WIFI", nullable = false)
    private boolean wifi;

    @Column(name="RESTROOM",nullable = false)
    private boolean restRoom;

    @Column(name="PARKING_AVAILABLE",nullable = false)
    private boolean parkingAvailable;

    @Column(name="PACKING_POSSIBLE",nullable = false)
    private boolean packingPossible;

    @Column(name="KAKAOPAY",nullable = false)
    private boolean kakaoPay;

    @Column(name = "SAMSUNGPAY",nullable = false)
    private boolean samsungPay;

    @Column(name = "KIOSK", nullable = false)
    private boolean kiosk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    private Restaurant restaurant;

}
