package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointCommandService.UseAllocationDetail;

public record PointUseAllocationResponse(
        String earningPointKey,
        Long usedAmount
) {
    public static PointUseAllocationResponse from(UseAllocationDetail detail) {
        return new PointUseAllocationResponse(detail.earningPointKey(), detail.usedAmount());
    }
}
