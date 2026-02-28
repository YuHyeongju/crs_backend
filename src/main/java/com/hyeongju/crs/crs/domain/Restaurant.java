package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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

    @Column(name = "REST_TEL", unique = true,length = 200)
    private String restTel;

    @Column(name = "REST_ADDRESS", nullable = false)
    private String restAddress;

    @Column(name = "REST_BUSI_HOURS", length = 100 )
    private String restBusiHours;

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL , orphanRemoval = true)
    private List<RestaurantMenu> menuList = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RestaurantFacilities> facilities = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookMark> bookMarks = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Congestion> congestions = new ArrayList<>();

    @Column(name = "KAKAO_ID", unique = true)
    private String kakaoId  ;

    @Column(nullable = false)
    private String status = "TEMP";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_IDX")
    @JsonIgnore
    private User user;

    @Column(name = "APPROVAL_STATUS", nullable = false)
    private String approvalStatus = "PENDING";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
