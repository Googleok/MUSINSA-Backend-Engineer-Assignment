package com.musinsa.freepoint.application;

import com.musinsa.freepoint.application.PointCommandService.EarnResult;
import com.musinsa.freepoint.application.PointCommandService.UseResult;
import com.musinsa.freepoint.application.PointQueryService.BalanceResult;
import com.musinsa.freepoint.application.PointQueryService.EarningsResult;
import com.musinsa.freepoint.application.PointQueryService.TransactionsResult;
import com.musinsa.freepoint.common.time.TestTimeProvider;
import com.musinsa.freepoint.config.TestTimeConfig;
import com.musinsa.freepoint.domain.earning.PointEarnType;
import com.musinsa.freepoint.domain.transaction.PointTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static com.musinsa.freepoint.common.time.TestTimeProvider.DEFAULT_BASE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestTimeConfig.class)
@Transactional
class PointQueryServiceTest {

    @Autowired
    PointCommandService pointCommandService;
    @Autowired
    PointQueryService pointQueryService;
    @Autowired
    TestTimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        timeProvider.setNow(DEFAULT_BASE_TIME);
    }

    @Test
    @DisplayName("balance 조회 - 적립/사용 후 잔액이 정확히 반영된다")
    void getBalance_after_earn_and_use() {
        pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);
        pointCommandService.usePoint("m1", "O-1", 300L);

        BalanceResult result = pointQueryService.getBalance("m1");

        assertThat(result.memberId()).isEqualTo("m1");
        assertThat(result.balance()).isEqualTo(700L);
    }

    @Test
    @DisplayName("balance 조회 - 미존재 회원은 0을 반환한다")
    void getBalance_returns_zero_for_unknown_member() {
        BalanceResult result = pointQueryService.getBalance("nobody");

        assertThat(result.memberId()).isEqualTo("nobody");
        assertThat(result.balance()).isZero();
    }

    @Test
    @DisplayName("earnings 조회 - 각 적립의 pointKey/타입/used/available이 정확히 반환된다")
    void getEarnings_returns_pointKey_and_amounts() {
        EarnResult normalEarn = pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);
        EarnResult manualEarn = pointCommandService.earn("m1", 500L, PointEarnType.MANUAL, null, null);
        // MANUAL이 우선 차감되어 manualEarn에서 300P 사용
        pointCommandService.usePoint("m1", "O-1", 300L);

        EarningsResult result = pointQueryService.getEarnings("m1");

        assertThat(result.memberId()).isEqualTo("m1");
        assertThat(result.earnings()).hasSize(2);

        var first = result.earnings().get(0);
        assertThat(first.pointKey()).isEqualTo(normalEarn.pointKey());
        assertThat(first.earnType()).isEqualTo(PointEarnType.NORMAL);
        assertThat(first.earnedAmount()).isEqualTo(1000L);
        assertThat(first.availableAmount()).isEqualTo(1000L);
        assertThat(first.usedAmount()).isZero();

        var second = result.earnings().get(1);
        assertThat(second.pointKey()).isEqualTo(manualEarn.pointKey());
        assertThat(second.earnType()).isEqualTo(PointEarnType.MANUAL);
        assertThat(second.earnedAmount()).isEqualTo(500L);
        assertThat(second.availableAmount()).isEqualTo(200L);
        assertThat(second.usedAmount()).isEqualTo(300L);
    }

    @Test
    @DisplayName("transactions 조회 - 모든 타입(EARN/USE/USE_CANCEL)이 시간순으로 반환된다")
    void getTransactions_returns_all_types_in_order() {
        pointCommandService.earn("m1", 1000L, PointEarnType.NORMAL, null, null);
        UseResult use = pointCommandService.usePoint("m1", "O-1", 300L);
        pointCommandService.cancelUse(use.pointKey(), "m1", 100L, null);

        TransactionsResult result = pointQueryService.getTransactions("m1");

        assertThat(result.memberId()).isEqualTo("m1");
        assertThat(result.transactions()).hasSize(3);
        assertThat(result.transactions().get(0).transactionType()).isEqualTo(PointTransactionType.EARN);
        assertThat(result.transactions().get(0).amount()).isEqualTo(1000L);
        assertThat(result.transactions().get(0).orderNo()).isNull();

        assertThat(result.transactions().get(1).transactionType()).isEqualTo(PointTransactionType.USE);
        assertThat(result.transactions().get(1).amount()).isEqualTo(300L);
        assertThat(result.transactions().get(1).orderNo()).isEqualTo("O-1");

        assertThat(result.transactions().get(2).transactionType()).isEqualTo(PointTransactionType.USE_CANCEL);
        assertThat(result.transactions().get(2).amount()).isEqualTo(100L);
        assertThat(result.transactions().get(2).orderNo()).isEqualTo("O-1");
        assertThat(result.transactions().get(2).originalTransactionId()).isNotNull();
    }

    @Test
    @DisplayName("미존재 회원의 earnings/transactions는 빈 리스트")
    void empty_collections_for_unknown_member() {
        EarningsResult earnings = pointQueryService.getEarnings("nobody");
        TransactionsResult transactions = pointQueryService.getTransactions("nobody");

        assertThat(earnings.earnings()).isEmpty();
        assertThat(transactions.transactions()).isEmpty();
    }
}
