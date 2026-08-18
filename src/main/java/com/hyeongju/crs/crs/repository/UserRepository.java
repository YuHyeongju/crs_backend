package com.hyeongju.crs.crs.repository;

import  com.hyeongju.crs.crs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Integer> {

    // 주의: JpaRepository의 existsById(Integer, PK 기준)와 매개변수 타입이 다른 오버로드
    // 여기서는 User의 "id"(로그인 아이디) 필드 기준 존재 여부 확인
    boolean existsById(String id);

    boolean existsByPhNum(String phNum);

    // 주의: JpaRepository의 findById(Integer)와 다른 오버로드 - 로그인 아이디 문자열로 조회
    Optional<User> findById(String id);

    Optional<User> findByUserIdx(int userIdx);

    boolean existsByBusinessNum(String businessNum);

    boolean existsByAdminNum(String adminNum);

    // 전체 유저를 role과 함께 즉시 로딩(JOIN FETCH) - 목록 순회 시 N+1 방지
    @Query("SELECT u FROM User u JOIN FETCH u.role")
    List<User> findAllWithRole();

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.userIdx = :userIdx")
    Optional<User> findByUserIdxWithRole(@Param("userIdx") int userIdx);

    // 아이디 찾기 기능에 사용
    Optional<User> findByNameAndEmail(String name, String email);

    // 비밀번호 재설정 기능에 사용
    Optional<User> findByIdAndEmail(String id, String email);
}
