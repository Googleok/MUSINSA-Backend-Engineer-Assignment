package com.musinsa.freepoint.application;

public enum RestoreType {
    /** 원 적립이 만료되지 않아 기존 적립 건에 복원 */
    ORIGINAL_EARNING,
    /** 원 적립이 이미 만료되어 신규 EXPIRED_RESTORE 적립을 생성 */
    NEW_EARNING
}
