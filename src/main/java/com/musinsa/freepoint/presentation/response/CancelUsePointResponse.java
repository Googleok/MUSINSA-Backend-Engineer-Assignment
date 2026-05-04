package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointCommandService.CancelUseResult;

import java.util.List;

public record CancelUsePointResponse(
        String pointKey,
        String originalUsePointKey,
        String memberId,
        Long canceledAmount,
        Long balance,
        List<RestoredAllocationResponse> restoredAllocations
) {
    public static CancelUsePointResponse from(CancelUseResult result) {
        List<RestoredAllocationResponse> restored = result.restoredAllocations().stream()
                .map(RestoredAllocationResponse::from)
                .toList();
        return new CancelUsePointResponse(
                result.pointKey(),
                result.originalUsePointKey(),
                result.memberId(),
                result.canceledAmount(),
                result.balance(),
                restored
        );
    }
}
