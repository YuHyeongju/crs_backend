package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role")
@Getter @Setter
@NoArgsConstructor
public class Role {
    // 유저 권한(USER/MERCHANT/ADMIN)을 나타내는 엔티티. User.role이 이 테이블을 참조함

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ROLE_IDX")
    private int roleIdx;

    @Enumerated(EnumType.STRING)
    @Column(name="ROLE_NAME", nullable = false,length = 100)
    private RoleName roleName;

    @OneToMany(mappedBy = "role",fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> users = new ArrayList<>(); // 이 역할을 가진 유저 목록
}
