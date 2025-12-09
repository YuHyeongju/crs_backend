package com.hyeongju.crs.crs.repository;

import  com.hyeongju.crs.crs.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, String> {
    //JPA가 SQL 자동 생성
}
