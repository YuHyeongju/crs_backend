package com.hyeongju.crs.crs.domain;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ROLE_IDX")
    private int roleIdx;

    @Enumerated(EnumType.STRING) // 열거형을 DB에 어떤 형태로 저장할지 지정
    @Column(name="ROLE_NAME", nullable = false,length = 100)
    private RoleName roleName;

    @OneToMany(mappedBy = "role",fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
    // 하나의 역할은 여러 사람에게 주어진다.
}
