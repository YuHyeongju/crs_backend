package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "congestion_level")
@NoArgsConstructor
public class CongestionLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONG_LEV_IDX")
    private int congLevIdx;

    @Column(name = "CONG_LEV_NAME",nullable = false,length = 50)
    private String congLevName;

    @OneToMany(mappedBy = "congLevel", fetch = FetchType.LAZY)
    private List<Congestion> congestions = new ArrayList<>();
    // Congestion 엔티티 목록을 참조하는 필드를 새로 추가
}
