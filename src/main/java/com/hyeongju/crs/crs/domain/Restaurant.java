package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="REST_IDX")
    @OneToMany(fetch = FetchType.LAZY)
    private int RestIdx;

    @Column(name="REST_NAME",nullable = false,length = 100)
    private String restName;

    @Column(name="REST_TEL", unique = true, nullable = false, length = 200)
    private String restTel;

    @Column(name="REST_ADDRESS", nullable = false)
    private String restAddress;

    @Column(name="REST_BUSI_HOURS", nullable = false, length = 100)
    private String restBusiHours;

    @Column(name="REST_MENU", nullable = false,length = 100)
    private String restMenu;

    @Column(name="REST_PRICE", nullable = false)
    private int restPrice;

    @Column(name="REST_MENU_PICT",length = 500)
    private String restMenuPict;



}
