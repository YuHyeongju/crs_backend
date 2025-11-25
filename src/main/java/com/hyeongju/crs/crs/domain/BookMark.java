package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "bookmark", uniqueConstraints = { // 복합 유니크 제약 조건 추가
        @UniqueConstraint(
                name = "UK_USER_RESTAURANT",
                columnNames = {"USER_IDX","REST_IDX"} // 두 컬럼의 조합이 유일해야함.
                // 한 유저가 한 식당에 여러 번 북마크를 할 수 없다.
        )
})
@NoArgsConstructor
public class BookMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookMarkIdx;

    @Column(name = "BOOKMARKED_AT",nullable = false)
    private LocalDateTime bookMarkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_IDX",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    private Restaurant restaurant;
}
