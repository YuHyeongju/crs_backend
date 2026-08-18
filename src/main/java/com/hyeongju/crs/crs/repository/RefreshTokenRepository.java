package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    // 토큰 문자열로 레코드 조회 (재발급 요청이 들어왔을 때 유효한 토큰인지 확인하기 위해 사용)
    Optional<RefreshToken> findByToken(String token);
    // 특정 유저의 리프레시 토큰을 전부 삭제 (로그아웃/재로그인 시 기존 토큰을 무효화하기 위해 사용)
    void deleteByUserIdx(int userIdx);
}
