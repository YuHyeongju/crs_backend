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
    // 일반 유저 회원가입/마이페이지 조회·수정 + 아이디 찾기/비밀번호 재설정 로직

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

    @Transactional
    public User registerUser(UserRegistractionDto dto){
        // 일반 유저 회원가입
        User newUser = super.registerCommonFields(dto, RoleName.USER);

        return userRepository.save(newUser);
    }

    public MypageResponseDto getUserProfile(int userIdx){
        // 마이페이지 조회: User 엔티티를 MypageResponseDto로 매핑
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
        // 마이페이지 수정: 비밀번호는 입력됐을 때만 재암호화 후 갱신
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()){
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
    }

    public void sendFindIdCode(String name, String email) {
        // 아이디 찾기 1단계: 이름+이메일이 일치하는 회원이 있는지 확인 후 인증코드 발송
        userRepository.findByNameAndEmail(name, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        String code = verificationService.generateAndStore("findId:" + email);
        emailService.sendVerificationCode(email, code);
    }

    public String verifyFindId(String name, String email, String code) {
        // 아이디 찾기 2단계: 인증코드 검증 후 마스킹된 아이디를 반환
        User user = userRepository.findByNameAndEmail(name, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        if (!verificationService.verify("findId:" + email, code)) {
            throw new RuntimeException("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        return maskId(user.getId()); // 아이디를 그대로 노출하지 않고 일부를 별표(*)로 가려서 반환
    }

    public void sendResetPasswordCode(String id, String email) {
        // 비밀번호 재설정 1단계: 아이디+이메일이 일치하는 회원이 있는지 확인 후 인증코드 발송
        userRepository.findByIdAndEmail(id, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        String code = verificationService.generateAndStore("resetPw:" + email);
        emailService.sendVerificationCode(email, code);
    }

    @Transactional
    public void verifyAndResetPassword(String id, String email, String code, String newPassword) {
        // 비밀번호 재설정 2단계: 인증코드 검증 후 새 비밀번호로 교체
        User user = userRepository.findByIdAndEmail(id, email)
                .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        if (!verificationService.verify("resetPw:" + email, code)) {
            throw new RuntimeException("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        user.setPw(passwordEncoder.encode(newPassword));
    }

    private String maskId(String id) {
        // 아이디 마스킹 규칙: 길이 2 이하면 첫 글자+"***", 그 외엔 앞 2글자 + 가운데를 전부 *로 + 마지막 1글자만 노출
        if (id.length() <= 2) return id.charAt(0) + "***";
        return id.substring(0, 2) + "*".repeat(id.length() - 3) + id.charAt(id.length() - 1);
    }
}
