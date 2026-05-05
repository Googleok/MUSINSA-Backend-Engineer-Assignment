package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointQueryService.EarningsResult;

import java.util.List;

public record EarningsResponse(
        String memberId,
        List<EarningItemResponse> earnings
) {
    public static EarningsResponse from(EarningsResult result) {
        List<EarningItemResponse> items = result.earnings().stream()
                .map(EarningItemResponse::from)
                .toList();
        return new EarningsResponse(result.memberId(), items);
    }
}
