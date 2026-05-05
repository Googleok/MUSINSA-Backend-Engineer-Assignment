package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointQueryService.TransactionItem;
import com.musinsa.freepoint.domain.transaction.PointTransactionType;

import java.time.LocalDateTime;

public record TransactionItemResponse(
        String pointKey,
        PointTransactionType transactionType,
        Long amount,
        String orderNo,
        Long originalTransactionId,
        LocalDateTime createdAt
) {
    public static TransactionItemResponse from(TransactionItem item) {
        return new TransactionItemResponse(
                item.pointKey(),
                item.transactionType(),
                item.amount(),
                item.orderNo(),
                item.originalTransactionId(),
                item.createdAt()
        );
    }
}
