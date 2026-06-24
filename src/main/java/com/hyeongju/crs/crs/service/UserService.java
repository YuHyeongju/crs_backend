package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends AbstractRegistrationService {

    private final EmailService emailService;
    private final VerificationService verificationService;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passWordEncoders,
        EmailService emailService,
        VerificationService verificationService
    ){
        super(userRepository, roleRepository, passWordEncoders);
        this.emailService = emailService;
        this.verificationService = verificationService;
    }

    @Transactional // 트랜잭션에 오류가 발생하면 작업이 취소되고 롤백되고 변경사항이 취소됨
    public User registerUser(UserRegistractionDto dto){
        // 부모한테서 registerCommonFields 호출
        User newUser = super.registerCommonFields(dto, RoleName.USER);

        return userRepository.save(newUser);
    }

    public MypageResponseDto getUserProfile(int userIdx){
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        MypageResponseDto dto = new MypageResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setRole(user.getRole().getRoleName().name());
        dto.setPhNum(user.getPhNum());
        return dto;
    }

    @Transactional
    public void updateUserProfile(int userIdx, UserUpdateDto dto){
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()){
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
    }

    public void sendFindIdCode(String name, String email) {
        userRepository.findByNameAndEmail(name, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        String code = verificationService.generateAndStore("findId:" + email);
        emailService.sendVerificationCode(email, code);
    }

    public String verifyFindId(String name, String email, String code) {
        User user = userRepository.findByNameAndEmail(name, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        if (!verificationService.verify("findId:" + email, code)) {
            throw new RuntimeException("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        return maskId(user.getId());
    }

    public void sendResetPasswordCode(String id, String email) {
        userRepository.findByIdAndEmail(id, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        String code = verificationService.generateAndStore("resetPw:" + email);
        emailService.sendVerificationCode(email, code);
    }

    @Transactional
    public void verifyAndResetPassword(String id, String email, String code, String newPassword) {
        User user = userRepository.findByIdAndEmail(id, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        if (!verificationService.verify("resetPw:" + email, code)) {
            throw new RuntimeException("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        user.setPw(passwordEncoder.encode(newPassword));
    }

    private String maskId(String id) {
        if (id.length() <= 2) return id.charAt(0) + "***";
        return id.substring(0, 2) + "*".repeat(id.length() - 3) + id.charAt(id.length() - 1);
    }
}
