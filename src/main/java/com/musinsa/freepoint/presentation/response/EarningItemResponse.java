package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointQueryService.EarningItem;
import com.musinsa.freepoint.domain.earning.PointEarnType;

import java.time.LocalDateTime;

public record EarningItemResponse(
        Long earningId,
        String pointKey,
        PointEarnType earnType,
        Long earnedAmount,
        Long availableAmount,
        Long usedAmount,
        Long canceledAmount,
        LocalDateTime expiresAt
) {
    public static EarningItemResponse from(EarningItem item) {
        return new EarningItemResponse(
                item.earningId(),
                item.pointKey(),
                item.earnType(),
                item.earnedAmount(),
                item.availableAmount(),
                item.usedAmount(),
                item.canceledAmount(),
                item.expiresAt()
        );
    }
}
