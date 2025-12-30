package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    // JPA가 SQL 자동 생성
    // 사용자에게 부여할 역할이 무엇인지 판단하는 코드
    Optional<Role> findByRoleName(RoleName roleName);


}
