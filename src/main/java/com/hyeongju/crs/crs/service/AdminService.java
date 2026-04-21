package com.hyeongju.crs.crs.service;


import com.hyeongju.crs.crs.domain.*;
import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.repository.CongestionRepository; // Import CongestionRepository
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.ReviewReportRepository; // Added
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService extends AbstractRegistrationService {

    private final RestaurantRepository restaurantRepository;
    private final CongestionRepository congestionRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository; // Added


    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RestaurantRepository restaurantRepository,
            CongestionRepository congestionRepository,
            ReviewRepository reviewRepository,
            ReviewReportRepository reviewReportRepository // Added
    ){
        super(userRepository,roleRepository,passwordEncoder);

        this.restaurantRepository = restaurantRepository;
        this.congestionRepository = congestionRepository;
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository; // Added
    }


    @Transactional
    public User registerAdmin(AdminRegistractionDto dto){
        if(!isVaildAdminNum(dto.getAdminNum())){
            throw new IllegalArgumentException("유효하지 않은 관리자 코드 입니다.");
        }

        User newUser = super.registerCommonFields(dto, RoleName.ADMIN);

        newUser.setAdminNum(dto.getAdminNum());

        return userRepository.save(newUser);
    }

    private boolean isVaildAdminNum(String adminNum){
        return adminNum != null && adminNum.length() == 7;
    }

    public MypageResponseDto getAdminProfile(int userIdx){
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
        return restaurantRepository.findByApprovalStatusAndStatus("PENDING", "ACTIVE");
    }
    @Transactional
    public void approvalRestaurant(int restIdx){
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new RuntimeException("가게를 찾을 수 없음"));

        restaurant.setApprovalStatus("APPROVED");
        restaurant.setStatus("ACTIVE");

        restaurantRepository.save(restaurant);
    }
    
    @Transactional
    public void rejectRestaurant(int restIdx){
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new RuntimeException("가게를 찾을 수 없음"));
        restaurant.setApprovalStatus("REJECT");
        restaurant.setStatus("ACTIVE");

        restaurantRepository.save(restaurant);
    }

    public List<UserListResponseDto> getAllUsers() {
        List<User> users = userRepository.findAllWithRole(); // Fetch all users with their roles

        return users.stream().map(user -> {
            long congestionCount = congestionRepository.countByUserUserIdx(user.getUserIdx());
            long reviewCount = reviewRepository.countByUserUserIdx(user.getUserIdx());
            return new UserListResponseDto(user, congestionCount, reviewCount);
        }).collect(Collectors.toList());
    }

    public UserDetailsResponseDto getUserDetails(int userIdx) {
        User user = userRepository.findByUserIdxWithRole(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        long congestionCount = congestionRepository.countByUserUserIdx(userIdx);
        long reviewCount = reviewRepository.countByUserUserIdx(userIdx);

        return new UserDetailsResponseDto(user, congestionCount, reviewCount);
    }

    @Transactional
    public void sanctionUser(int userIdx, String reason) {
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        user.setStatus("SUSPENDED");
        // Optionally, you might want to store the reason and sanction duration
        // user.setSanctionReason(reason);
        // user.setSanctionEndDate(LocalDateTime.now().plusDays(7)); // Example: 7-day suspension
        userRepository.save(user); // Save the updated user status
    }

    @Transactional
    public void deactivateUser(int userIdx) {
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        user.setStatus("DEACTIVATED");
        userRepository.save(user); // Save the updated user status
    }

    // Review Report Management
    public List<ReviewReport> getAllReviewReports() {
        return reviewReportRepository.findAll();
    }

    public ReviewReport getReviewReportDetails(int reportIdx) {
        return reviewReportRepository.findById(reportIdx)
                .orElseThrow(() -> new RuntimeException("Review report not found."));
    }

    @Transactional
    public void processReviewReport(int reportIdx, boolean approve) {
        ReviewReport report = reviewReportRepository.findById(reportIdx)
                .orElseThrow(() -> new RuntimeException("Review report not found."));

        if (approve) {
            // Approve the report: change review status and report status
            Review reportedReview = report.getReportedReview();
            reportedReview.setStatus("BLOCKED"); // Or "HIDDEN", "DELETED" based on policy
            reviewRepository.save(reportedReview);

            report.setStatus("APPROVED");
        } else {
            // Reject the report: change report status
            report.setStatus("REJECTED");
        }
        reviewReportRepository.save(report);
    }
}
