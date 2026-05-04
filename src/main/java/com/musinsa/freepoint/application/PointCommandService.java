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
import com.musinsa.freepoint.domain.wallet.MemberPointWallet;
import com.musinsa.freepoint.domain.wallet.MemberPointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointCommandService {

    private final MemberPointWalletRepository walletRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointEarningRepository earningRepository;
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
}
