package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "bookmark", uniqueConstraints = {
        @UniqueConstraint(
                name = "UK_USER_RESTAURANT",
                columnNames = {"USER_IDX","REST_IDX"}
                // 한 유저가 한 식당에 여러 번 북마크할 수 없도록 강제
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
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_IDX",nullable = false)
    @JsonIgnore
    private Restaurant restaurant;
}
