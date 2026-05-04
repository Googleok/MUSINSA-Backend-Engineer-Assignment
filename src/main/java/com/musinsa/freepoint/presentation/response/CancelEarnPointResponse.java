package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointCommandService.CancelEarnResult;

public record CancelEarnPointResponse(
        String pointKey,
        String originalPointKey,
        String memberId,
        Long canceledAmount,
        Long balance
) {
    public static CancelEarnPointResponse from(CancelEarnResult result) {
        return new CancelEarnPointResponse(
                result.pointKey(),
                result.originalPointKey(),
                result.memberId(),
                result.canceledAmount(),
                result.balance()
        );
    }
}
