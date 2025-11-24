package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "congestion_level")
@NoArgsConstructor
public class CongestionLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @OneToMany(fetch = FetchType.LAZY)
    private int congLevIdx;

    @Column(name = "CONG_LEV_NAME",nullable = false,length = 50)
    private String congLevName;
}
