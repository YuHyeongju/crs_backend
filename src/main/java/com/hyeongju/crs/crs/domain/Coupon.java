package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COUPON_IDX")
    private int couponIdx;

    // 쿠폰을 발행한 가게 (상인이 본인 소유 가게에 등록)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX", nullable = false)
    @JsonIgnore
    private Restaurant restaurant;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    // 교환에 필요한 포인트
    @Column(name = "POINT_COST", nullable = false)
    private int pointCost;

    // 유효기간 (null이면 무기한)
    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

    // 노출 여부 (상인이 내리면 false → 소프트 삭제)
    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
