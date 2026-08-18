package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
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
    // cascade=ALL: 식당 저장/삭제 시 메뉴도 함께 처리됨. orphanRemoval=true: 리스트에서 빼면 DB에서도 삭제됨
    @JsonIgnoreProperties({"restaurant"})              // 메뉴->식당->메뉴 순환 참조 방지
    private List<RestaurantMenu> menuList = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"restaurant"})
    private List<RestaurantFacilities> facilities = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookMark> bookMarks = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @BatchSize(size = 10) // 지연 로딩 시 N+1 방지 (최대 10개씩 묶어 IN 절로 조회)
    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Congestion> congestions = new ArrayList<>();

    @Column(name = "KAKAO_ID", unique = true)
    private String kakaoId  ;                          // 카카오 지도 API 연동용 식당 식별자

    @Column(nullable = false)
    private String status = "TEMP";                    // TEMP=카카오에서 자동 생성된 임시 데이터

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "User_IDX")
    @JsonIgnore
    private User user;                                  // 등록/소유 상인 (카카오 자동생성 식당은 null 가능)

    @Column(name = "APPROVAL_STATUS", nullable = false)
    private String approvalStatus = "PENDING";

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "LATITUDE")
    private Double latitude;

    @Column(name = "LONGITUDE")
    private Double longitude;
}
