package com.musinsa.freepoint.application;

import com.musinsa.freepoint.common.exception.BusinessException;
import com.musinsa.freepoint.common.exception.ErrorCode;
import com.musinsa.freepoint.common.time.TimeProvider;
import com.musinsa.freepoint.domain.earning.PointEarnType;
import com.musinsa.freepoint.domain.earning.PointEarning;
import com.musinsa.freepoint.domain.earning.PointEarningRepository;
import com.musinsa.freepoint.domain.transaction.PointTransaction;
import com.musinsa.freepoint.domain.transaction.PointTransactionRepository;
import com.musinsa.freepoint.domain.transaction.PointTransactionType;
import com.musinsa.freepoint.domain.usage.PointUseAllocation;
import com.musinsa.freepoint.domain.usage.PointUseAllocationRepository;
import com.musinsa.freepoint.domain.wallet.MemberPointWallet;
import com.musinsa.freepoint.domain.wallet.MemberPointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointCommandService {

    private final MemberPointWalletRepository walletRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointEarningRepository earningRepository;
    private final PointUseAllocationRepository useAllocationRepository;
    private final PointPolicyService pointPolicyService;
    private final PointKeyGenerator pointKeyGenerator;
    private final TimeProvider timeProvider;

    @Transactional
    public EarnResult earn(
            String memberId,
            long amount,
            PointEarnType earnType,
            Integer expireDays,
            String reason
    ) {
        validateEarnAmount(amount);
        int resolvedExpireDays = resolveExpireDays(expireDays);

        MemberPointWallet wallet = getOrCreateWallet(memberId);
        validateWalletBalanceLimit(wallet.getBalance(), amount);

        LocalDateTime now = timeProvider.now();
        LocalDateTime expiresAt = now.plusDays(resolvedExpireDays);

        String pointKey = pointKeyGenerator.generate();
        PointTransaction transaction = transactionRepository.save(
                PointTransaction.earn(pointKey, memberId, amount, reason, null)
        );

        PointEarning earning = earningRepository.save(
                PointEarning.create(transaction.getId(), memberId, earnType, amount, expiresAt)
        );

        wallet.increaseBalance(amount);

        return new EarnResult(
                transaction.getPointKey(),
                wallet.getMemberId(),
                amount,
                wallet.getBalance(),
                earning.getExpiresAt()
        );
    }

    @Transactional
    public CancelEarnResult cancelEarn(String originalPointKey, String memberId, String reason) {
        PointTransaction original = transactionRepository.findByPointKey(originalPointKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.EARNING_NOT_FOUND,
                        "적립 내역을 찾을 수 없습니다. pointKey=" + originalPointKey));

        if (original.getTransactionType() != PointTransactionType.EARN) {
            throw new BusinessException(ErrorCode.EARNING_NOT_FOUND,
                    "적립 트랜잭션이 아닙니다. pointKey=" + originalPointKey);
        }
        if (!original.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.EARNING_NOT_FOUND,
                    "회원 정보가 일치하지 않습니다. pointKey=" + originalPointKey);
        }

        MemberPointWallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "지갑을 찾을 수 없습니다. memberId=" + memberId));

        PointEarning earning = earningRepository.findByTransactionId(original.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EARNING_NOT_FOUND,
                        "적립 단위를 찾을 수 없습니다. transactionId=" + original.getId()));

        if (earning.isUsed()) {
            throw new BusinessException(ErrorCode.EARNING_ALREADY_USED);
        }
        if (earning.getCanceledAmount() > 0L) {
            throw new BusinessException(ErrorCode.EARNING_ALREADY_CANCELED);
        }

        long cancelAmount = earning.getAvailableAmount();

        String cancelPointKey = pointKeyGenerator.generate();
        PointTransaction cancelTransaction = transactionRepository.save(
                PointTransaction.earnCancel(
                        cancelPointKey,
                        memberId,
                        cancelAmount,
                        original.getId(),
                        reason,
                        null
                )
        );

        earning.cancel();
        wallet.decreaseBalance(cancelAmount);

        return new CancelEarnResult(
                cancelTransaction.getPointKey(),
                original.getPointKey(),
                memberId,
                cancelAmount,
                wallet.getBalance()
        );
    }

    @Transactional
    public CancelUseResult cancelUse(String originalUsePointKey, String memberId, long amount, String reason) {
        if (amount < 1L) {
            throw new BusinessException(ErrorCode.INVALID_CANCEL_AMOUNT,
                    "취소 금액은 1 이상이어야 합니다. amount=" + amount);
        }

        PointTransaction original = transactionRepository.findByPointKey(originalUsePointKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.USE_TRANSACTION_NOT_FOUND,
                        "사용 내역을 찾을 수 없습니다. pointKey=" + originalUsePointKey));

        if (original.getTransactionType() != PointTransactionType.USE) {
            throw new BusinessException(ErrorCode.USE_TRANSACTION_NOT_FOUND,
                    "사용 트랜잭션이 아닙니다. pointKey=" + originalUsePointKey);
        }
        if (!original.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.USE_TRANSACTION_NOT_FOUND,
                    "회원 정보가 일치하지 않습니다. pointKey=" + originalUsePointKey);
        }

        MemberPointWallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "지갑을 찾을 수 없습니다. memberId=" + memberId));

        long alreadyCanceled = useAllocationRepository.sumCanceledAmountByUseTransactionId(original.getId());
        long cancelable = original.getAmount() - alreadyCanceled;
        if (amount > cancelable) {
            throw new BusinessException(ErrorCode.USE_CANCEL_AMOUNT_EXCEEDED,
                    "취소 가능 금액을 초과했습니다. 요청=" + amount + ", 취소가능=" + cancelable);
        }

        LocalDateTime now = timeProvider.now();
        int defaultExpireDays = pointPolicyService.getDefaultExpireDays();

        List<PointUseAllocation> allocations =
                useAllocationRepository.findByUseTransactionIdOrderByIdAsc(original.getId());

        List<RestoredAllocationDetail> restored = new ArrayList<>();
        long remaining = amount;

        for (PointUseAllocation alloc : allocations) {
            if (remaining <= 0L) {
                break;
            }
            long allocCancelable = alloc.getCancelableAmount();
            if (allocCancelable <= 0L) {
                continue;
            }
            long take = Math.min(remaining, allocCancelable);

            PointEarning earning = earningRepository.findById(alloc.getEarningId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.EARNING_NOT_FOUND,
                            "적립 단위를 찾을 수 없습니다. earningId=" + alloc.getEarningId()));

            PointTransaction earningTx = transactionRepository.findById(earning.getTransactionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                            "원 적립 트랜잭션을 찾을 수 없습니다. id=" + earning.getTransactionId()));
            String originalEarningPointKey = earningTx.getPointKey();

            if (earning.isExpired(now)) {
                String newEarnPointKey = pointKeyGenerator.generate();
                PointTransaction newEarnTransaction = transactionRepository.save(
                        PointTransaction.earn(newEarnPointKey, memberId, take, reason, null)
                );
                LocalDateTime newExpiresAt = now.plusDays(defaultExpireDays);
                earningRepository.save(
                        PointEarning.create(
                                newEarnTransaction.getId(),
                                memberId,
                                PointEarnType.EXPIRED_RESTORE,
                                take,
                                newExpiresAt
                        )
                );
                alloc.cancel(take);
                restored.add(new RestoredAllocationDetail(
                        originalEarningPointKey,
                        RestoreType.NEW_EARNING,
                        take,
                        newEarnTransaction.getPointKey()
                ));
            } else {
                earning.restore(take);
                alloc.cancel(take);
                restored.add(new RestoredAllocationDetail(
                        originalEarningPointKey,
                        RestoreType.ORIGINAL_EARNING,
                        take,
                        null
                ));
            }

            remaining -= take;
        }

        if (remaining > 0L) {
            throw new BusinessException(ErrorCode.USE_CANCEL_AMOUNT_EXCEEDED,
                    "내부 오류: 미처리 취소 금액이 남아있습니다. remaining=" + remaining);
        }

        String cancelPointKey = pointKeyGenerator.generate();
        PointTransaction useCancelTransaction = transactionRepository.save(
                PointTransaction.useCancel(
                        cancelPointKey,
                        memberId,
                        amount,
                        original.getOrderNo(),
                        original.getId(),
                        reason,
                        null
                )
        );

        wallet.increaseBalance(amount);

        return new CancelUseResult(
                useCancelTransaction.getPointKey(),
                original.getPointKey(),
                memberId,
                amount,
                wallet.getBalance(),
                restored
        );
    }

    @Transactional
    public UseResult usePoint(String memberId, String orderNo, long amount) {
        if (amount < 1L) {
            throw new BusinessException(ErrorCode.INVALID_USE_AMOUNT);
        }
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(ErrorCode.ORDER_NO_REQUIRED);
        }

        MemberPointWallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_POINT,
                        "지갑을 찾을 수 없습니다. memberId=" + memberId));

        if (wallet.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT,
                    "보유 포인트가 부족합니다. balance=" + wallet.getBalance() + ", amount=" + amount);
        }

        LocalDateTime now = timeProvider.now();
        List<PointEarning> usableEarnings = earningRepository.findUsableEarnings(memberId, now);

        List<EarningDeduction> deductions = new ArrayList<>();
        long remaining = amount;
        for (PointEarning earning : usableEarnings) {
            if (remaining <= 0L) {
                break;
            }
            long take = Math.min(earning.getAvailableAmount(), remaining);
            deductions.add(new EarningDeduction(earning, take));
            remaining -= take;
        }

        if (remaining > 0L) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT,
                    "사용 가능한 미만료 적립이 부족합니다. 요청=" + amount + ", 부족=" + remaining);
        }

        String useTxKey = pointKeyGenerator.generate();
        PointTransaction useTransaction = transactionRepository.save(
                PointTransaction.use(useTxKey, memberId, amount, orderNo, null, null)
        );

        for (EarningDeduction d : deductions) {
            d.earning().use(d.amount());
            useAllocationRepository.save(
                    PointUseAllocation.create(
                            useTransaction.getId(),
                            d.earning().getId(),
                            memberId,
                            orderNo,
                            d.amount()
                    )
            );
        }

        wallet.decreaseBalance(amount);

        Set<Long> earningTxIds = new HashSet<>();
        for (EarningDeduction d : deductions) {
            earningTxIds.add(d.earning().getTransactionId());
        }
        Map<Long, String> earningTxIdToPointKey = transactionRepository.findAllById(earningTxIds).stream()
                .collect(Collectors.toMap(PointTransaction::getId, PointTransaction::getPointKey));

        List<UseAllocationDetail> details = deductions.stream()
                .map(d -> new UseAllocationDetail(
                        earningTxIdToPointKey.get(d.earning().getTransactionId()),
                        d.amount()
                ))
                .toList();

        return new UseResult(
                useTransaction.getPointKey(),
                memberId,
                orderNo,
                amount,
                wallet.getBalance(),
                details
        );
    }

    private record EarningDeduction(PointEarning earning, long amount) {
    }

    private void validateEarnAmount(long amount) {
        if (amount < 1L) {
            throw new BusinessException(ErrorCode.INVALID_EARN_AMOUNT,
                    "적립 가능 포인트는 1 이상이어야 합니다. amount=" + amount);
        }
        long maxEarnPerOnce = pointPolicyService.getMaxEarnAmountPerOnce();
        if (amount > maxEarnPerOnce) {
            throw new BusinessException(ErrorCode.INVALID_EARN_AMOUNT,
                    "1회 최대 적립 가능 포인트를 초과했습니다. amount=" + amount + ", max=" + maxEarnPerOnce);
        }
    }

    private int resolveExpireDays(Integer requestedExpireDays) {
        int expireDays = (requestedExpireDays != null)
                ? requestedExpireDays
                : pointPolicyService.getDefaultExpireDays();
        int minExpireDays = pointPolicyService.getMinExpireDays();
        int maxExpireDays = pointPolicyService.getMaxExpireDays();
        if (expireDays < minExpireDays || expireDays > maxExpireDays) {
            throw new BusinessException(ErrorCode.INVALID_EXPIRE_DAYS,
                    "만료일은 " + minExpireDays + " ~ " + maxExpireDays + "일 범위여야 합니다. expireDays=" + expireDays);
        }
        return expireDays;
    }

    private MemberPointWallet getOrCreateWallet(String memberId) {
        return walletRepository.findByMemberIdForUpdate(memberId)
                .orElseGet(() -> walletRepository.save(MemberPointWallet.create(memberId)));
    }

    private void validateWalletBalanceLimit(long currentBalance, long amount) {
        long maxWalletBalance = pointPolicyService.getMaxWalletBalance();
        if (currentBalance + amount > maxWalletBalance) {
            throw new BusinessException(ErrorCode.BALANCE_LIMIT_EXCEEDED,
                    "보유 가능한 최대 포인트를 초과했습니다. balance=" + currentBalance + ", amount=" + amount + ", max=" + maxWalletBalance);
        }
    }

    public record EarnResult(
            String pointKey,
            String memberId,
            long amount,
            long balance,
            LocalDateTime expiresAt
    ) {
    }

    public record CancelEarnResult(
            String pointKey,
            String originalPointKey,
            String memberId,
            long canceledAmount,
            long balance
    ) {
    }

    public record UseResult(
            String pointKey,
            String memberId,
            String orderNo,
            long usedAmount,
            long balance,
            List<UseAllocationDetail> allocations
    ) {
    }

    public record UseAllocationDetail(
            String earningPointKey,
            long usedAmount
    ) {
    }

    public record CancelUseResult(
            String pointKey,
            String originalUsePointKey,
            String memberId,
            long canceledAmount,
            long balance,
            List<RestoredAllocationDetail> restoredAllocations
    ) {
    }

    public record RestoredAllocationDetail(
            String earningPointKey,
            RestoreType restoreType,
            long restoredAmount,
            String newEarningPointKey
    ) {
    }
}
