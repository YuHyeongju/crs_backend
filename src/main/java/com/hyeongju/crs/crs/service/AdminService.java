package com.hyeongju.crs.crs.service;


import com.hyeongju.crs.crs.domain.*;
import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.repository.CongestionRepository;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.ReviewReportRepository;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService extends AbstractRegistrationService {
    // 관리자 회원가입 + 관리자 전용 기능(가게 승인/거절, 유저 목록/제재, 리뷰 신고 처리) 담당 서비스

    private final RestaurantRepository restaurantRepository;
    private final CongestionRepository congestionRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;


    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RestaurantRepository restaurantRepository,
            CongestionRepository congestionRepository,
            ReviewRepository reviewRepository,
            ReviewReportRepository reviewReportRepository
    ){
        super(userRepository,roleRepository,passwordEncoder);

        this.restaurantRepository = restaurantRepository;
        this.congestionRepository = congestionRepository;
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
    }


    @Transactional
    public User registerAdmin(AdminRegistractionDto dto){
        // 관리자 회원가입: 관리자 인증코드 검증 → 공통 필드 생성 → 인증코드 세팅 → 저장
        if(!isVaildAdminNum(dto.getAdminNum())){
            throw new IllegalArgumentException("유효하지 않은 관리자 코드 입니다.");
        }

        User newUser = super.registerCommonFields(dto, RoleName.ADMIN);

        newUser.setAdminNum(dto.getAdminNum());

        return userRepository.save(newUser);
    }

    private boolean isVaildAdminNum(String adminNum){
        return adminNum != null && adminNum.length() == 7; // 형식(영문+숫자) 자체는 DTO의 @Pattern에서 이미 검증됨
    }

    public MypageResponseDto getAdminProfile(int userIdx){
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
        dto.setAdminNum(user.getAdminNum());
        return dto;
    }

    @Transactional
    public void updateAdminProfile(int userIdx, AdminUpdateDto dto){
        // 마이페이지 수정: 비밀번호는 입력됐을 때만 재암호화 후 갱신
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()) {
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
        user.setAdminNum(dto.getAdminNum());
    }

    public List<Restaurant> getPendingRestaurant(){
        // 승인 대기중(PENDING)이면서 아직 유효(ACTIVE)한 식당 목록 — 관리자 승인 화면용
        return restaurantRepository.findByApprovalStatusAndStatus("PENDING", "ACTIVE");
    }
    @Transactional
    public void approvalRestaurant(int restIdx){
        // 대기중인 가게 등록 신청을 승인 처리
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new RuntimeException("가게를 찾을 수 없음"));

        restaurant.setApprovalStatus("APPROVED");
        restaurant.setStatus("ACTIVE");

        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void rejectRestaurant(int restIdx){
        // 대기중인 가게 등록 신청을 거절 처리
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new RuntimeException("가게를 찾을 수 없음"));
        restaurant.setApprovalStatus("REJECT");
        restaurant.setStatus("ACTIVE");

        restaurantRepository.save(restaurant);
    }

    public List<UserListResponseDto> getAllUsers() {
        // 관리자 화면의 전체 유저 목록 (유저별 제보/리뷰 건수 포함)
        List<User> users = userRepository.findAllWithRole();

        return users.stream().map(user -> {
            // 유저마다 제보/리뷰 건수를 별도 COUNT 쿼리로 구해서 DTO에 함께 담음
            long congestionCount = congestionRepository.countByUserUserIdx(user.getUserIdx());
            long reviewCount = reviewRepository.countByUserUserIdx(user.getUserIdx());
            return new UserListResponseDto(user, congestionCount, reviewCount);
        }).collect(Collectors.toList());
    }

    public UserDetailsResponseDto getUserDetails(int userIdx) {
        // 관리자 화면의 유저 상세 정보 조회
        User user = userRepository.findByUserIdxWithRole(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        long congestionCount = congestionRepository.countByUserUserIdx(userIdx);
        long reviewCount = reviewRepository.countByUserUserIdx(userIdx);

        return new UserDetailsResponseDto(user, congestionCount, reviewCount);
    }

    @Transactional
    public void sanctionUser(int userIdx, String reason) {
        // 유저 제재(정지) 처리 — 현재는 사유/기간을 별도로 저장하지 않고 상태만 SUSPENDED로 변경
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        user.setStatus("SUSPENDED");
        userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(int userIdx) {
        // 유저 비활성화 처리(제재보다 강한 조치 — 상태를 DEACTIVATED로 변경)
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        user.setStatus("DEACTIVATED");
        userRepository.save(user);
    }

    public List<ReviewReportResponseDto> getAllReviewReports() {
        // 전체 리뷰 신고 목록 (상태 필터 없이 전부 반환)
        return reviewReportRepository.findAll().stream()
                .map(ReviewReportResponseDto::new)
                .collect(Collectors.toList());
    }

    public ReviewReportResponseDto getReviewReportDetails(int reportIdx) {
        // 리뷰 신고 상세 조회
        ReviewReport report = reviewReportRepository.findById(reportIdx)
                .orElseThrow(() -> new RuntimeException("Review report not found."));
        return new ReviewReportResponseDto(report);
    }

    @Transactional
    public void processReviewReport(int reportIdx, boolean approve) {
        // 승인이면 신고당한 리뷰를 BLOCKED로 바꾸고 신고도 APPROVED로,
        // 반려면 신고 상태만 REJECTED로 바꾸고 리뷰는 그대로 둠
        ReviewReport report = reviewReportRepository.findById(reportIdx)
                .orElseThrow(() -> new RuntimeException("Review report not found."));

        if (approve) {
            Review reportedReview = report.getReportedReview();
            reportedReview.setStatus("BLOCKED");
            reviewRepository.save(reportedReview);

            report.setStatus("APPROVED");
        } else {
            report.setStatus("REJECTED");
        }
        reviewReportRepository.save(report);
    }
}
