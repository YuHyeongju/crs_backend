package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "congestion")
@NoArgsConstructor
public class Congestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONG_IDX")
    private int congIdx;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONG_LEVEL", nullable = false)
    private CongestionLevel congLevel;
    // 외래키를 참조하는 필드는 참조 대상의 엔티티 타입으로 선언해야함.
    // ID 값 매핑에서 엔티티 객체 매핑으로 되었기에 referencedColumnName는 필요 없어짐.

    @Column(name = "CONG_AT",nullable = false)
    private LocalDateTime congAt;

    @Column(name = "CONG_SHOW",nullable = false)
    private boolean congShow;

    @Column(name = "CONG_UPDATE_AT")
    private LocalDateTime congUpdateAt;

    @Column(name = "CONG_HIDDEN_AT")
    private LocalDateTime congHiddenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    private Restaurant restaurant;


}
