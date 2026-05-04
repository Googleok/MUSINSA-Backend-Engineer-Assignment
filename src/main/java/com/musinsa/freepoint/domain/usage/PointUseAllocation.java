package com.musinsa.freepoint.domain.usage;

import com.musinsa.freepoint.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "point_use_allocation",
        indexes = {
                @Index(name = "idx_point_use_allocation_use_tx_id", columnList = "use_transaction_id"),
                @Index(name = "idx_point_use_allocation_earning_id", columnList = "earning_id")
        }
)
public class PointUseAllocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "use_transaction_id", nullable = false)
    private Long useTransactionId;

    @Column(name = "earning_id", nullable = false)
    private Long earningId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "used_amount", nullable = false)
    private Long usedAmount;

    @Column(name = "canceled_amount", nullable = false)
    private Long canceledAmount;

    private PointUseAllocation(
            Long useTransactionId,
            Long earningId,
            String memberId,
            String orderNo,
            long usedAmount
    ) {
        this.useTransactionId = useTransactionId;
        this.earningId = earningId;
        this.memberId = memberId;
        this.orderNo = orderNo;
        this.usedAmount = usedAmount;
        this.canceledAmount = 0L;
    }

    public static PointUseAllocation create(
            Long useTransactionId,
            Long earningId,
            String memberId,
            String orderNo,
            long usedAmount
    ) {
        if (useTransactionId == null) {
            throw new IllegalArgumentException("useTransactionId는 필수입니다.");
        }
        if (earningId == null) {
            throw new IllegalArgumentException("earningId는 필수입니다.");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("memberId는 필수입니다.");
        }
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("orderNo는 필수입니다.");
        }
        if (usedAmount <= 0L) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        return new PointUseAllocation(useTransactionId, earningId, memberId, orderNo, usedAmount);
    }

    public long getCancelableAmount() {
        return this.usedAmount - this.canceledAmount;
    }

    public void cancel(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다.");
        }
        if (amount > getCancelableAmount()) {
            throw new IllegalStateException("취소 가능 금액을 초과했습니다.");
        }
        this.canceledAmount += amount;
    }
}
