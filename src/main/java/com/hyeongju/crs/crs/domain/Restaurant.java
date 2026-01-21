package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "restaurant")
@NoArgsConstructor
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REST_IDX")
    private int restIdx;

    @Column(name = "REST_NAME", nullable = false, length = 100)
    private String restName;

    @Column(name = "REST_TEL", unique = true, nullable = false, length = 200)
    private String restTel;

    @Column(name = "REST_ADDRESS", nullable = false)
    private String restAddress;

    @Column(name = "REST_BUSI_HOURS", nullable = false, length = 100)
    private String restBusiHours;

    @Column(name = "REST_MENU", nullable = false, length = 100)
    private String restMenu;

    @Column(name = "REST_PRICE", nullable = false)
    private int restPrice;

    @Column(name = "REST_MENU_PICT", length = 500)
    private String restMenuPict;

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    private List<RestaurantFacilities> facilities = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    private List<BookMark> bookMarks = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Congestion> congestions = new ArrayList<>();

    @Column(name = "KAKAO_ID", unique = true, nullable = false)
    private String kakaoId  ;

}
