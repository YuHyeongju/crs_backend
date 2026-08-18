package com.hyeongju.crs.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade; // 미사용 import

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_IDX")
    private int userIdx;

    @Column(name="ID", unique = true , nullable = false, length = 50)
    private String id;

    @Column(name = "PW", nullable = false , length = 255)
    @JsonIgnore                                        // 비밀번호 해시는 JSON 응답에 노출되면 안 됨
    private String pw;

    @Column(name ="EMAIL", nullable = false, length = 200)
    private String email;

    @Column(name="NAME",nullable = false, length = 100)
    private String name;

    @Column(name ="PNUM", unique = true, nullable = false , length = 100)
    private String phNum;

    @Column(name="GENDER", nullable = false, length = 100)
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_IDX", nullable = false)
    private Role role;

    @Column(name="BUSINESS_NUM",length = 200)
    private String businessNum;                        // 사업자 등록번호(상인 회원가입 시 사용)

    @Column(name="ADMIN_NUM",length = 100)
    private String adminNum;                           // 관리자 인증 번호(관리자 회원가입 시 사용)

    @Column(name= "CREATE_TIME")
    private LocalDateTime createTime;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, DEACTIVATED, WITHDRAWN

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BookMark> bookMarks = new ArrayList<>();

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Reward> rewards = new ArrayList<>();

    @OneToMany(mappedBy = "reporter",fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ReviewReport> reviewReports = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Congestion> congestions = new ArrayList<>();

}
