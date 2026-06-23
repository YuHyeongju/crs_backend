package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false, length = 500)
    private String token;

    @Column(name = "user_idx", nullable = false)
    private int userIdx;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
