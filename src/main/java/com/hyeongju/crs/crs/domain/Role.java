package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role")
@Getter @Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ROLE_IDX")
    @OneToMany(fetch = FetchType.LAZY)
    private int roleIdx;

    @Column(name="ROLE_NAME", nullable = false,length = 100)
    private String roleName;
}
