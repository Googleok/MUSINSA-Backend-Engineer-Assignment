package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointCommandService.EarnResult;

import java.time.LocalDateTime;

public record EarnPointResponse(
        String pointKey,
        String memberId,
        Long amount,
        Long balance,
        LocalDateTime expiresAt
) {
    public static EarnPointResponse from(EarnResult result) {
        return new EarnPointResponse(
                result.pointKey(),
                result.memberId(),
                result.amount(),
                result.balance(),
                result.expiresAt()
        );
    }
}
