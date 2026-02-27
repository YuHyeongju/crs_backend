package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "restaurant_menu")

public class RestaurantMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MENU_IDX")
    private int menuIdx;

    @Column(name = "MENU_NAME", nullable = false)
    private String menuName;

    @Column(name ="MENU_PRICE", nullable = false)
    private Integer menuPrice;

    @Column(name="MENU_PICT", length = 500)
    private String menuPict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX")
    private Restaurant restaurant;
}
