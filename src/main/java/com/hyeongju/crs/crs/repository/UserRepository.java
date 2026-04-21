package com.hyeongju.crs.crs.repository;

import  com.hyeongju.crs.crs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Added for @Query
import org.springframework.data.repository.query.Param;

import java.util.List; // Added for List return type
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Integer> {
    
    // ID 필드를 기준으로 중복 확인
    boolean existsById(String id);
    
    // 휴대폰 번호 필드(PNum)를 기준으로 중복 확인
    boolean existsByPhNum(String phNum);

    // ID 필드를 기준으로 사용자 조회
    Optional<User> findById(String id);

    Optional<User> findByUserIdx(int userIdx);

    // 사업자 등록 번호를 기준으로 중복 확인
    boolean existsByBusinessNum(String businessNum);

    // 관리자 코드를 기준으로 중복 확인
    boolean existsByAdminNum(String adminNum);

    void deleteById(String id);

    @Query("SELECT u FROM User u JOIN FETCH u.role")
    List<User> findAllWithRole();

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.userIdx = :userIdx")
    Optional<User> findByUserIdxWithRole(@Param("userIdx") int userIdx);
}
