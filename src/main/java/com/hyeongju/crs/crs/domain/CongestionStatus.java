package com.hyeongju.crs.crs.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CongestionStatus {

    FREE(1, "여유"),
    NORMAL(2,"보통"),
    BUSY(3,"혼잡"),
    VERY_BUSY(4,"매우 혼잡"),
    NONE(0,"혼잡도 이력 없음");

    private final int idx;
    private final String name;

    public static CongestionStatus convertIdx(int idx){
        // 프론트에서 넘어온 정수 코드를 CongestionStatus enum으로 변환 (해당하는 값이 없으면 NONE)
        for(CongestionStatus status : values()){
            if(status.getIdx() == idx){
                return status;
            }
        }
        return NONE;
    }
}
