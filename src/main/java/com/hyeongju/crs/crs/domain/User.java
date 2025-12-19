package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor; // 기본 생성자를 생성
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user") // 실제 db 테이블 이름 지정
@Getter @Setter
@NoArgsConstructor  // 기본 생성자 자동 생성
public class User {

    @Id // PK를 나타내기 위한 어노테이션 
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // 기본키의 값이 자동으로 생성
    // 생성 전략을 정의하기 위해 사용 기본 auto, TABLE, SEQUENCE, IDENTIFY가 있음
    // IDENTIFY : 기본키 생성 작업을 DB에게 위임
    // SEQUENCE : DB에 있는 시퀀스를 사용 하는 방법, DB에 미리 시퀀스를 생성해줘야함. 필요할 떄 마다 시퀀스 호출 - 성능 저하
    // TABLE: 시퀀스를 지원하지 않는 DB에서 시퀀스 처럼 사용하고 싶을 때 사용하는 것.
    @Column(name = "USER_IDX") // 컬럼 이름을 명시함. 자바 필드명과 DB컬럼명이 다를 때 사용
    private int userIdx;

    @Column(name="ID", unique = true , nullable = false, length = 50)
    private String id;

    @Column(name = "PW", nullable = false , length = 255)
    private String pw;

    @Column(name ="EMAIL", nullable = false, length = 200)
    private String email;

    @Column(name="NAME",nullable = false, length = 100)
    private String name;

    @Column(name ="PNUM", unique = true, nullable = false , length = 100)
    private String phNum;

    @Column(name="GENDER", nullable = false, length = 100)
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "ROLE_IDX", nullable = false)
    private Role role;
    // 한 사람은 하나의 역할이 주어진다.

    @Column(name="BUSINESS_NUM",length = 200)
    private String businessNum;

    @Column(name="ADMIN_NUM",length = 100)
    private String adminNum;

    @Column(name= "CREATE_TIME")
    private LocalDateTime createTime;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    private List<BookMark> bookMarks = new ArrayList<>();

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY)
    private List<Reward> rewards = new ArrayList<>();

    @OneToMany(mappedBy = "reporter",fetch = FetchType.LAZY)
    private List<ReviewReport> reviewReports = new ArrayList<>();

    // mappedBy에는 many to one 으로 참조하는 컬럼의 필드 이름을 작성
    // 내 주인은 저 컬럼 입니다를 알려주는 것.
    // 이 리스트를 채우려면 Congestion 테이블에 있는 user 필드가 가르키는 외래키를 조회해라
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Congestion> congestions = new ArrayList<>();

}
