package com.musinsa.freepoint.application;

import com.musinsa.freepoint.domain.earning.PointEarnType;
import com.musinsa.freepoint.domain.earning.PointEarning;
import com.musinsa.freepoint.domain.earning.PointEarningRepository;
import com.musinsa.freepoint.domain.transaction.PointTransaction;
import com.musinsa.freepoint.domain.transaction.PointTransactionRepository;
import com.musinsa.freepoint.domain.transaction.PointTransactionType;
import com.musinsa.freepoint.domain.wallet.MemberPointWallet;
import com.musinsa.freepoint.domain.wallet.MemberPointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointQueryService {

    private final MemberPointWalletRepository walletRepository;
    private final PointEarningRepository earningRepository;
    private final PointTransactionRepository transactionRepository;

    public BalanceResult getBalance(String memberId) {
        long balance = walletRepository.findByMemberId(memberId)
                .map(MemberPointWallet::getBalance)
                .orElse(0L);
        return new BalanceResult(memberId, balance);
    }

    public EarningsResult getEarnings(String memberId) {
        List<PointEarning> earnings = earningRepository.findByMemberIdOrderByIdAsc(memberId);

        Set<Long> transactionIds = new HashSet<>();
        for (PointEarning e : earnings) {
            transactionIds.add(e.getTransactionId());
        }
        Map<Long, String> transactionIdToPointKey = transactionRepository.findAllById(transactionIds).stream()
                .collect(Collectors.toMap(PointTransaction::getId, PointTransaction::getPointKey));

        List<EarningItem> items = earnings.stream()
                .map(e -> new EarningItem(
                        e.getId(),
                        transactionIdToPointKey.get(e.getTransactionId()),
                        e.getEarnType(),
                        e.getEarnedAmount(),
                        e.getAvailableAmount(),
                        e.getUsedAmount(),
                        e.getCanceledAmount(),
                        e.getExpiresAt()
                ))
                .toList();

        return new EarningsResult(memberId, items);
    }

    public TransactionsResult getTransactions(String memberId) {
        List<PointTransaction> transactions = transactionRepository.findByMemberIdOrderByIdAsc(memberId);

        List<TransactionItem> items = transactions.stream()
                .map(t -> new TransactionItem(
                        t.getPointKey(),
                        t.getTransactionType(),
                        t.getAmount(),
                        t.getOrderNo(),
                        t.getOriginalTransactionId(),
                        t.getCreatedAt()
                ))
                .toList();

        return new TransactionsResult(memberId, items);
    }

    public record BalanceResult(
            String memberId,
            long balance
    ) {
    }

    public record EarningsResult(
            String memberId,
            List<EarningItem> earnings
    ) {
    }

    public record EarningItem(
            Long earningId,
            String pointKey,
            PointEarnType earnType,
            long earnedAmount,
            long availableAmount,
            long usedAmount,
            long canceledAmount,
            LocalDateTime expiresAt
    ) {
    }

    public record TransactionsResult(
            String memberId,
            List<TransactionItem> transactions
    ) {
    }

    public record TransactionItem(
            String pointKey,
            PointTransactionType transactionType,
            long amount,
            String orderNo,
            Long originalTransactionId,
            LocalDateTime createdAt
    ) {
    }
}
