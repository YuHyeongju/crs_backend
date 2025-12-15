package com.hyeongju.crs.crs.repository;

import  com.hyeongju.crs.crs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, String> {
    
    // ID 필드를 기준으로 중복 확인
    boolean existsById(String id);
    
    // 휴대폰 번호 필드(PNum)를 기준으로 중복 확인
    boolean existsByPNum(String PNum);

    // ID 필드를 기준으로 사용자 조회
    Optional<User> findById(String id);
}
