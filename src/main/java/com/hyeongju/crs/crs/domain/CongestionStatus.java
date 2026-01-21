package com.hyeongju.crs.crs.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CongestionStatus {

    FREE(1, "여유"),
    NORMAL(2,"보통"),
    BUSY(3,"혼잡"),
    VERY_BUSY(4,"매우 혼잡");

    private final int idx;
    private final String name;

    // 숫자를 인자로 주면 해당하는 상수를 찾아줌
    public static CongestionStatus convertIdx(int idx){
        for(CongestionStatus status : values()){
            if(status.getIdx() == idx){
                return status;
            }
        }
        return NORMAL;
    }
}
