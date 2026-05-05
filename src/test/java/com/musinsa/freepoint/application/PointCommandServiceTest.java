package com.musinsa.freepoint.application;

import com.musinsa.freepoint.application.PointCommandService.CancelEarnResult;
import com.musinsa.freepoint.application.PointCommandService.CancelUseResult;
import com.musinsa.freepoint.application.PointCommandService.EarnResult;
import com.musinsa.freepoint.application.PointCommandService.RestoredAllocationDetail;
import com.musinsa.freepoint.application.PointCommandService.UseResult;
import com.musinsa.freepoint.common.exception.BusinessException;
import com.musinsa.freepoint.common.exception.ErrorCode;
import com.musinsa.freepoint.common.time.TestTimeProvider;
import com.musinsa.freepoint.config.TestTimeConfig;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.musinsa.freepoint.common.time.TestTimeProvider.DEFAULT_BASE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestTimeConfig.class)
@Transactional
class PointCommandServiceTest {

    @Autowired
    PointCommandService pointCommandService;
    @Autowired
    MemberPointWalletRepository walletRepository;
    @Autowired
    PointEarningRepository earningRepository;
    @Autowired
    PointTransactionRepository transactionRepository;
    @Autowired
    PointUseAllocationRepository useAllocationRepository;
    @Autowired
    TestTimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        timeProvider.setNow(DEFAULT_BASE_TIME);
    }

    // ============================================================
    // 1. 적립 성공
    // ============================================================

    @Test
    @DisplayName("적립 성공 - 1000P 적립 시 wallet/transaction/earning이 일관되게 생성된다")
    void earn_success_creates_wallet_transaction_earning() {
        EarnResult result = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, 365, "EVENT");

        assertThat(result.amount()).isEqualTo(1000L);
        assertThat(result.balance()).isEqualTo(1000L);
        assertThat(result.expiresAt()).isEqualTo(DEFAULT_BASE_TIME.plusDays(365));

        MemberPointWallet wallet = walletRepository.findByMemberId("m1").orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(1000L);

        PointTransaction tx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
        assertThat(tx.getTransactionType()).isEqualTo(PointTransactionType.EARN);
        assertThat(tx.getAmount()).isEqualTo(1000L);

        PointEarning earning = earningRepository.findByTransactionId(tx.getId()).orElseThrow();
        assertThat(earning.getEarnType()).isEqualTo(PointEarnType.NORMAL);
        assertThat(earning.getEarnedAmount()).isEqualTo(1000L);
        assertThat(earning.getAvailableAmount()).isEqualTo(1000L);
        assertThat(earning.getUsedAmount()).isZero();
        assertThat(earning.getCanceledAmount()).isZero();
    }

    @Test
    @DisplayName("적립 성공 - expireDays가 null이면 기본 정책 365일이 적용된다")
    void earn_uses_default_expire_days_when_null() {
        EarnResult result = pointCommandService.earn("m1", 100L, PointEarnType.NORMAL, null, null);

        assertThat(result.expiresAt()).isEqualTo(DEFAULT_BASE_TIME.plusDays(365));
    }

    // ============================================================
    // 2. 적립 실패
    // ============================================================

    @Test
    @DisplayName("적립 실패 - amount가 0이면 INVALID_EARN_AMOUNT")
    void earn_fail_when_amount_is_zero() {
        assertThatThrownBy(() -> pointCommandService.earn("m1", 0L, PointEarnType.NORMAL, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EARN_AMOUNT);
    }

    @Test
    @DisplayName("적립 실패 - 1회 최대 적립 한도 초과 시 INVALID_EARN_AMOUNT")
    void earn_fail_when_amount_exceeds_max_per_once() {
        assertThatThrownBy(() -> pointCommandService.earn("m1", 200_000L, PointEarnType.NORMAL, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EARN_AMOUNT);
    }

    @Test
    @DisplayName("적립 실패 - 회원당 보유 한도 초과 시 BALANCE_LIMIT_EXCEEDED")
    void earn_fail_when_wallet_balance_limit_exceeded() {
        for (int i = 0; i < 10; i++) {
            pointCommandService.earn("m1", 100_000L, PointEarnType.NORMAL, null, null);
        }
        // 누적 1,000,000 도달. 추가 적립 1P도 한도 초과.
        assertThatThrownBy(() -> pointCommandService.earn("m1", 1L, PointEarnType.NORMAL, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BALANCE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("적립 실패 - expireDays가 0이면 INVALID_EXPIRE_DAYS")
    void earn_fail_when_expireDays_is_zero() {
        assertThatThrownBy(() -> pointCommandService.earn("m1", 100L, PointEarnType.NORMAL, 0, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EXPIRE_DAYS);
    }

    @Test
    @DisplayName("적립 실패 - expireDays가 1825이면 INVALID_EXPIRE_DAYS (5년 미만 정책)")
    void earn_fail_when_expireDays_too_large() {
        assertThatThrownBy(() -> pointCommandService.earn("m1", 100L, PointEarnType.NORMAL, 1825, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EXPIRE_DAYS);
    }

    // ============================================================
    // 3. 관리자 수기 적립
    // ============================================================

    @Test
    @DisplayName("관리자 수기 적립 - earnType MANUAL이 그대로 저장된다")
    void earn_with_manual_type() {
        EarnResult result = pointCommandService.earn("m1", 500L, PointEarnType.MANUAL, null, "ADMIN");

        PointTransaction tx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
        PointEarning earning = earningRepository.findByTransactionId(tx.getId()).orElseThrow();
        assertThat(earning.getEarnType()).isEqualTo(PointEarnType.MANUAL);
    }

    // ============================================================
    // 4. 적립취소 성공
    // ============================================================

    @Test
    @DisplayName("적립취소 성공 - 미사용 적립은 잔액 복구되고 EARN_CANCEL이 생성된다")
    void cancelEarn_success_unused_earning() {
        EarnResult earn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);

        CancelEarnResult cancel = pointCommandService.cancelEarn(earn.pointKey(), "m1", "ADMIN_CANCEL");

        assertThat(cancel.canceledAmount()).isEqualTo(1000L);
        assertThat(cancel.balance()).isZero();
        assertThat(cancel.originalPointKey()).isEqualTo(earn.pointKey());

        MemberPointWallet wallet = walletRepository.findByMemberId("m1").orElseThrow();
        assertThat(wallet.getBalance()).isZero();

        PointTransaction earnTx = transactionRepository.findByPointKey(earn.pointKey()).orElseThrow();
        PointEarning earning = earningRepository.findByTransactionId(earnTx.getId()).orElseThrow();
        assertThat(earning.getAvailableAmount()).isZero();
        assertThat(earning.getCanceledAmount()).isEqualTo(1000L);

        PointTransaction cancelTx = transactionRepository.findByPointKey(cancel.pointKey()).orElseThrow();
        assertThat(cancelTx.getTransactionType()).isEqualTo(PointTransactionType.EARN_CANCEL);
        assertThat(cancelTx.getOriginalTransactionId()).isEqualTo(earnTx.getId());
        assertThat(cancelTx.getAmount()).isEqualTo(1000L);
    }

    // ============================================================
    // 5. 적립취소 실패
    // ============================================================

    @Test
    @DisplayName("적립취소 실패 - 일부라도 사용된 적립은 EARNING_ALREADY_USED")
    void cancelEarn_fail_after_partial_use() {
        EarnResult earn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);
        pointCommandService.usePoint("m1", "O-1", 100L);

        assertThatThrownBy(() -> pointCommandService.cancelEarn(earn.pointKey(), "m1", null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EARNING_ALREADY_USED);
    }

    @Test
    @DisplayName("적립취소 실패 - 이미 취소된 적립을 재취소하면 EARNING_ALREADY_CANCELED")
    void cancelEarn_fail_already_canceled() {
        EarnResult earn = pointCommandService.earn("m1", 500L, PointEarnType.NORMAL, null, null);
        pointCommandService.cancelEarn(earn.pointKey(), "m1", null);

        assertThatThrownBy(() -> pointCommandService.cancelEarn(earn.pointKey(), "m1", null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EARNING_ALREADY_CANCELED);
    }

    // ============================================================
    // 6. 사용 성공
    // ============================================================

    @Test
    @DisplayName("사용 성공 - USE 트랜잭션과 PointUseAllocation이 일관되게 생성된다")
    void use_success_creates_transaction_and_allocation() {
        EarnResult earn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);

        UseResult result = pointCommandService.usePoint("m1", "O-1", 300L);

        assertThat(result.usedAmount()).isEqualTo(300L);
        assertThat(result.balance()).isEqualTo(700L);
        assertThat(result.allocations()).hasSize(1);
        assertThat(result.allocations().get(0).earningPointKey()).isEqualTo(earn.pointKey());
        assertThat(result.allocations().get(0).usedAmount()).isEqualTo(300L);

        PointTransaction useTx = transactionRepository.findByPointKey(result.pointKey()).orElseThrow();
        assertThat(useTx.getTransactionType()).isEqualTo(PointTransactionType.USE);
        assertThat(useTx.getOrderNo()).isEqualTo("O-1");
        assertThat(useTx.getAmount()).isEqualTo(300L);

        List<PointUseAllocation> allocations =
                useAllocationRepository.findByUseTransactionIdOrderByIdAsc(useTx.getId());
        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).getOrderNo()).isEqualTo("O-1");
        assertThat(allocations.get(0).getUsedAmount()).isEqualTo(300L);
        assertThat(allocations.get(0).getCanceledAmount()).isZero();
    }

    // ============================================================
    // 7. 사용 우선순위
    // ============================================================

    @Test
    @DisplayName("사용 우선순위 - MANUAL이 NORMAL보다 만료가 늦어도 먼저 사용된다")
    void use_priority_manual_first() {
        EarnResult normalShort = pointCommandService.earn("m1", 500L, PointEarnType.NORMAL, 10, null);
        EarnResult manualLong = pointCommandService.earn("m1", 800L, PointEarnType.MANUAL, 1000, null);

        UseResult result = pointCommandService.usePoint("m1", "O-1", 1000L);

        assertThat(result.allocations()).hasSize(2);
        assertThat(result.allocations().get(0).earningPointKey()).isEqualTo(manualLong.pointKey());
        assertThat(result.allocations().get(0).usedAmount()).isEqualTo(800L);
        assertThat(result.allocations().get(1).earningPointKey()).isEqualTo(normalShort.pointKey());
        assertThat(result.allocations().get(1).usedAmount()).isEqualTo(200L);
    }

    @Test
    @DisplayName("사용 우선순위 - 같은 타입이면 만료가 빠른 적립이 먼저 사용된다")
    void use_priority_expiresAt_order_within_same_type() {
        EarnResult longExp = pointCommandService.earn("m1", 500L, PointEarnType.NORMAL, 100, null);
        EarnResult shortExp = pointCommandService.earn("m1", 500L, PointEarnType.NORMAL, 10, null);

        UseResult result = pointCommandService.usePoint("m1", "O-1", 100L);

        assertThat(result.allocations()).hasSize(1);
        assertThat(result.allocations().get(0).earningPointKey()).isEqualTo(shortExp.pointKey());
        // longExp는 사용되지 않아야 한다.
        assertThat(result.allocations().get(0).earningPointKey()).isNotEqualTo(longExp.pointKey());
    }

    // ============================================================
    // 8. 사용 실패
    // ============================================================

    @Test
    @DisplayName("사용 실패 - 주문번호가 비어있으면 IllegalArgumentException (도메인 검증)")
    void use_fail_when_orderNo_is_blank() {
        pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);

        assertThatThrownBy(() -> pointCommandService.usePoint("m1", "  ", 100L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("사용 실패 - 잔액 부족 시 INSUFFICIENT_POINT")
    void use_fail_when_balance_insufficient() {
        pointCommandService.earn("m1", 100L, PointEarnType.NORMAL, null, null);

        assertThatThrownBy(() -> pointCommandService.usePoint("m1", "O-1", 200L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
    }

    @Test
    @DisplayName("사용 실패 - 적립이 모두 만료된 경우 INSUFFICIENT_POINT")
    void use_fail_when_only_expired_earnings_available() {
        pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, 10, null);
        timeProvider.advanceDays(20);

        assertThatThrownBy(() -> pointCommandService.usePoint("m1", "O-1", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
    }

    // ============================================================
    // 9. 사용취소 성공 - 원 적립이 만료되지 않은 경우
    // ============================================================

    @Test
    @DisplayName("사용취소 성공(미만료) - 원 적립이 ORIGINAL_EARNING으로 복원된다")
    void cancelUse_success_when_earning_not_expired() {
        EarnResult earn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, 365, null);
        UseResult use = pointCommandService.usePoint("m1", "O-1", 1000L);

        CancelUseResult cancel = pointCommandService.cancelUse(use.pointKey(), "m1", 300L, null);

        assertThat(cancel.canceledAmount()).isEqualTo(300L);
        assertThat(cancel.balance()).isEqualTo(300L);
        assertThat(cancel.restoredAllocations()).hasSize(1);
        RestoredAllocationDetail r = cancel.restoredAllocations().get(0);
        assertThat(r.restoreType()).isEqualTo(RestoreType.ORIGINAL_EARNING);
        assertThat(r.earningPointKey()).isEqualTo(earn.pointKey());
        assertThat(r.restoredAmount()).isEqualTo(300L);
        assertThat(r.newEarningPointKey()).isNull();

        // 원 적립이 복원됨
        PointTransaction earnTx = transactionRepository.findByPointKey(earn.pointKey()).orElseThrow();
        PointEarning earning = earningRepository.findByTransactionId(earnTx.getId()).orElseThrow();
        assertThat(earning.getAvailableAmount()).isEqualTo(300L);
        assertThat(earning.getUsedAmount()).isEqualTo(700L);

        // allocation의 canceledAmount가 증가
        PointTransaction useTx = transactionRepository.findByPointKey(use.pointKey()).orElseThrow();
        List<PointUseAllocation> allocations =
                useAllocationRepository.findByUseTransactionIdOrderByIdAsc(useTx.getId());
        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).getCanceledAmount()).isEqualTo(300L);

        // USE_CANCEL 트랜잭션 생성
        PointTransaction cancelTx = transactionRepository.findByPointKey(cancel.pointKey()).orElseThrow();
        assertThat(cancelTx.getTransactionType()).isEqualTo(PointTransactionType.USE_CANCEL);
        assertThat(cancelTx.getOriginalTransactionId()).isEqualTo(useTx.getId());
        assertThat(cancelTx.getOrderNo()).isEqualTo("O-1");
    }

    // ============================================================
    // 10. 사용취소 성공 - 원 적립이 만료된 경우
    // ============================================================

    @Test
    @DisplayName("사용취소 성공(만료) - EXPIRED_RESTORE 타입의 신규 적립이 생성된다")
    void cancelUse_success_when_earning_expired_creates_new_earning() {
        EarnResult earn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, 10, null);
        UseResult use = pointCommandService.usePoint("m1", "O-1", 1000L);

        // 만료 시점을 지나도록 시간 이동
        timeProvider.advanceDays(20);

        CancelUseResult cancel = pointCommandService.cancelUse(use.pointKey(), "m1", 500L, "EXP_RESTORE");

        assertThat(cancel.canceledAmount()).isEqualTo(500L);
        assertThat(cancel.balance()).isEqualTo(500L);
        assertThat(cancel.restoredAllocations()).hasSize(1);
        RestoredAllocationDetail r = cancel.restoredAllocations().get(0);
        assertThat(r.restoreType()).isEqualTo(RestoreType.NEW_EARNING);
        assertThat(r.earningPointKey()).isEqualTo(earn.pointKey());
        assertThat(r.restoredAmount()).isEqualTo(500L);
        assertThat(r.newEarningPointKey()).isNotNull();

        // 원 적립은 복원되지 않아야 함
        PointTransaction earnTx = transactionRepository.findByPointKey(earn.pointKey()).orElseThrow();
        PointEarning original = earningRepository.findByTransactionId(earnTx.getId()).orElseThrow();
        assertThat(original.getAvailableAmount()).isZero();
        assertThat(original.getUsedAmount()).isEqualTo(1000L);

        // 신규 EARN 트랜잭션 + 신규 EXPIRED_RESTORE 적립 생성
        PointTransaction newEarnTx = transactionRepository.findByPointKey(r.newEarningPointKey()).orElseThrow();
        assertThat(newEarnTx.getTransactionType()).isEqualTo(PointTransactionType.EARN);
        assertThat(newEarnTx.getAmount()).isEqualTo(500L);

        PointEarning newEarning = earningRepository.findByTransactionId(newEarnTx.getId()).orElseThrow();
        assertThat(newEarning.getEarnType()).isEqualTo(PointEarnType.EXPIRED_RESTORE);
        assertThat(newEarning.getEarnedAmount()).isEqualTo(500L);
        assertThat(newEarning.getAvailableAmount()).isEqualTo(500L);
        assertThat(newEarning.getExpiresAt())
                .isEqualTo(timeProvider.now().plusDays(365));
    }

    // ============================================================
    // 11. 사용취소 실패
    // ============================================================

    @Test
    @DisplayName("사용취소 실패 - 취소 가능 금액 초과 시 USE_CANCEL_AMOUNT_EXCEEDED")
    void cancelUse_fail_when_amount_exceeds_cancelable() {
        pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);
        UseResult use = pointCommandService.usePoint("m1", "O-1", 500L);

        assertThatThrownBy(() -> pointCommandService.cancelUse(use.pointKey(), "m1", 600L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USE_CANCEL_AMOUNT_EXCEEDED);
    }

    @Test
    @DisplayName("사용취소 실패 - USE 외 트랜잭션 키로 시도하면 USE_TRANSACTION_NOT_FOUND")
    void cancelUse_fail_when_transaction_is_not_USE() {
        EarnResult earn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);

        assertThatThrownBy(() -> pointCommandService.cancelUse(earn.pointKey(), "m1", 100L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USE_TRANSACTION_NOT_FOUND);
    }
}
