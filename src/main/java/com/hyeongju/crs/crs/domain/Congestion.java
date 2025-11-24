package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "congestion")
@NoArgsConstructor
public class Congestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int congIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",referencedColumnName = "REST_IDX",nullable = false)
    private int restIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",referencedColumnName = "USER_IDX",nullable = false)
    private int userIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONG_LEVEL",referencedColumnName = "CONG_LEV_IDX",nullable = false)
    private int congLevel;

    @Column(name = "CONG_AT",nullable = false)
    private LocalDateTime congAt;

    @Column(name = "CONG_SHOW",nullable = false)
    private boolean congShow;

    @Column(name = "CONG_UPDATE_AT")
    private LocalDateTime congUpdateAt;

    @Column(name = "CONG_HIDDEN_AT")
    private LocalDateTime congHiddenAt;

}
