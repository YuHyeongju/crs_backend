package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsById(String id){
        return userRepository.existsById(id);
    }

    public boolean existsByPhone(String pNum){
        return userRepository.existsByPNum(pNum);
    }

    @Transactional // 트랜잭션 단위 관리를 스프링이 대신하게 지시
    public void registerUser(UserRegistractionDto dto){
        // 비밀번호 암호화
        String encodePassword = passwordEncoder.encode(dto.getPw());
        
        //DTO 데이터 엔터티로 매핑 저장
        User user = new User();
        user.setId(dto.getId());
        user.setPw(encodePassword);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPNum(dto.getPhone());
        user.setGender(dto.getGender());

        user.setCreateTime(LocalDateTime.now()); // 생성 시간

        userRepository.save(user);

    }

    // 로그인 인증 메서드
    public void authenticate(String id, String rawPassword){
        // rawPassword = 암호화되지않은 사용자가입력한 패스워드
        User user = userRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("존재하지 않는 사용자 ID 입니다."));
                // orElseThrow는 인자가 null일 경우 예외처리 시킴

        if(!passwordEncoder.matches(rawPassword, user.getPw())){
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
            // 예외 강제 발생
        }

    }

    public void logout(){

    }



}
