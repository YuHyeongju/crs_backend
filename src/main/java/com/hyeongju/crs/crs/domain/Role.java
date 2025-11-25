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

    @Column(name="ROLE_NAME", nullable = false,length = 100)
    private String roleName;

    @OneToMany(mappedBy = "role",fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
    // 하나의 역할은 여러 사람에게 주어진다.
}
