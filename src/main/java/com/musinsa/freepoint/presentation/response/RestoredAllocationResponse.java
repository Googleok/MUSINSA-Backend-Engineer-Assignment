package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointCommandService.RestoredAllocationDetail;
import com.musinsa.freepoint.application.RestoreType;

public record RestoredAllocationResponse(
        String earningPointKey,
        RestoreType restoreType,
        Long restoredAmount,
        String newEarningPointKey
) {
    public static RestoredAllocationResponse from(RestoredAllocationDetail detail) {
        return new RestoredAllocationResponse(
                detail.earningPointKey(),
                detail.restoreType(),
                detail.restoredAmount(),
                detail.newEarningPointKey()
        );
    }
}
