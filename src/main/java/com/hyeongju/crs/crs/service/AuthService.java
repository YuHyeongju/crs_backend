package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    private final RoleRepository roleRepository;

    public boolean existsById(String id){
        return userRepository.existsById(id);
    }

    public boolean existsByPhone(String pNum){
        return userRepository.existsByPhNum(pNum);
    }
    public boolean existsByBusinessNum(String businessNum){
        return userRepository.existsByBusinessNum(businessNum);
    }

    public boolean existsByAdminNum(String adminNum){
        return userRepository.existsByAdminNum(adminNum);
    }


    private Role getRoleByName(RoleName roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName(roleName);
                    return roleRepository.save(newRole);
                });
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
        user.setPhNum(dto.getPhone());
        user.setGender(dto.getGender());

        user.setCreateTime(LocalDateTime.now()); // 생성 시간

        user.setRole(getRoleByName(RoleName.USER));// 역할

        userRepository.save(user);

    }

    @Transactional
    public void registerMerchant(MerchantRegistractionDto dto){
        // 비밀번호 암호화
        String encodePassword = passwordEncoder.encode(dto.getPw());

        //DTO 데이터 엔터티로 매핑 저장
        User user = new User();
        user.setId(dto.getId());
        user.setPw(encodePassword);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhNum(dto.getPhone());
        user.setGender(dto.getGender());
        user.setBusinessNum(dto.getBusinessNum());

        user.setCreateTime(LocalDateTime.now()); // 생성 시간

        user.setRole(getRoleByName(RoleName.MERCHANT)); // 역할

        userRepository.save(user);
    }

    @Transactional
    public void registerAdmin(AdminRegistractionDto dto){
        // 비밀번호 암호화
        String encodePassword = passwordEncoder.encode(dto.getPw());

        //DTO 데이터 엔터티로 매핑 저장
        User user = new User();
        user.setId(dto.getId());
        user.setPw(encodePassword);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhNum(dto.getPhone());
        user.setGender(dto.getGender());
        user.setAdminNum(dto.getAdminNum());

        user.setCreateTime(LocalDateTime.now()); // 생성 시간

        user.setRole(getRoleByName(RoleName.ADMIN));

        userRepository.save(user);
    }

    // 로그인 인증 메서드
    public User authenticate(String id, String rawPassword){
        // rawPassword = 암호화되지않은 사용자가입력한 패스워드
        User user = userRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("존재하지 않는 사용자 ID 입니다."));
                // orElseThrow는 인자가 null일 경우 예외처리 시킴

        if(!passwordEncoder.matches(rawPassword, user.getPw())){
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
            // 예외 강제 발생
        }
        return user;
    }

    public void logout(HttpServletRequest request){
        HttpSession session  = request.getSession(false);

        if(session != null){
            session.invalidate(); // 서버 메모리에서 세션 삭제
        }
    }

    @Transactional // 회원 탈퇴시에 DB에서 정보 삭제와 세션 삭제가 동시에 이루어져야함.
    public void withdraw(String id){
        // DB에서 사용자 삭제
        if(userRepository.existsById(id)){
            userRepository.deleteById(id);
        }else{
            throw new RuntimeException("존재하지 않는 사용자 입니다.");
        }

    }



}
