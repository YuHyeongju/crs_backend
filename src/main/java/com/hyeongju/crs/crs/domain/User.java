package com.hyeongju.crs.crs.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor; // 기본 생성자를 생성
import lombok.Setter;

import java.time.LocalDateTime;

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
    private Integer userIdx;

    @Column(name="ID", unique = true , nullable = false, length = 50)
    private String id;

    @Column(name = "PW", nullable = false , length = 100)
    private String pw;

    @Column(name ="EMAIL", nullable = false, length = 200)
    private String email;

    @Column(name="NAME",nullable = false, length = 100)
    private String name;

    @Column(name ="PNUM", unique = true, nullable = false , length = 100)
    private String pNum;

    @Column(name="GENDER", nullable = false, length = 100)
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE", referencedColumnName = "ROLE_IDX", nullable = false)
    private Role role;

    @Column(name="BUSINESS_NUM",length = 200)
    private String businessNum;

    @Column(name="ADMIN_NUM",length = 100)
    private String adminNum;

    @Column(name= "CREATE_TIME")
    private LocalDateTime createTime;


}
