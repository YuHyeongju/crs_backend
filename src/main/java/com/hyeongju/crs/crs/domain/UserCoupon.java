package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupon")
@Getter
@Setter
@NoArgsConstructor
public class UserCoupon {
    // 유저가 포인트로 교환해 보유 중인 쿠폰 1건. Coupon(발행 정보)과 User(보유자)를 이어주는 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_COUPON_IDX")
    private int userCouponIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUPON_IDX", nullable = false)
    private Coupon coupon;

    @Column(name = "USED", nullable = false)
    private boolean used = false; // 쿠폰 사용 여부

    @CreationTimestamp
    @Column(name = "ISSUED_AT", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;
}
