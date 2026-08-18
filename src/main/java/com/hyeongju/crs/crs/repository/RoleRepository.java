package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    // roleName으로 Role 엔티티 조회 (없으면 Optional.empty — 호출부가 새로 생성해서 저장함)
    Optional<Role> findByRoleName(RoleName roleName);

}
