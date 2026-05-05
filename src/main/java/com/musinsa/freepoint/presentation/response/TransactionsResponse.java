package com.musinsa.freepoint.presentation.response;

import com.musinsa.freepoint.application.PointQueryService.TransactionsResult;

import java.util.List;

public record TransactionsResponse(
        String memberId,
        List<TransactionItemResponse> transactions
) {
    public static TransactionsResponse from(TransactionsResult result) {
        List<TransactionItemResponse> items = result.transactions().stream()
                .map(TransactionItemResponse::from)
                .toList();
        return new TransactionsResponse(result.memberId(), items);
    }
}
