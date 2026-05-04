package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointCommandService.UseResult;

import java.util.List;

public record UsePointResponse(
        String pointKey,
        String memberId,
        String orderNo,
        Long usedAmount,
        Long balance,
        List<PointUseAllocationResponse> allocations
) {
    public static UsePointResponse from(UseResult result) {
        List<PointUseAllocationResponse> allocations = result.allocations().stream()
                .map(PointUseAllocationResponse::from)
                .toList();
        return new UsePointResponse(
                result.pointKey(),
                result.memberId(),
                result.orderNo(),
                result.usedAmount(),
                result.balance(),
                allocations
        );
    }
}
