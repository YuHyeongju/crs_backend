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

    // 사용 여부
    @Column(name = "USED", nullable = false)
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "ISSUED_AT", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;
}
