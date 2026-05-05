package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointQueryService.BalanceResult;

public record BalanceResponse(
        String memberId,
        Long balance
) {
    public static BalanceResponse from(BalanceResult result) {
        return new BalanceResponse(result.memberId(), result.balance());
    }
}
