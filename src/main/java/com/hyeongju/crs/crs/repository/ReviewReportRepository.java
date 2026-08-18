package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

// 커스텀 조회 메서드 없이 JpaRepository가 기본 제공하는 save/findAll/findById/delete 등만 그대로 사용
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Integer> {
}
