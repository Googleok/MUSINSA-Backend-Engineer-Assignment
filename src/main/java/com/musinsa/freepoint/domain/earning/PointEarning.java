package com.musinsa.freepoint.domain.earning;

import com.musinsa.freepoint.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "point_earning",
        indexes = {
                @Index(name = "idx_point_earning_member_id", columnList = "member_id"),
                @Index(name = "idx_point_earning_transaction_id", columnList = "transaction_id")
        }
)
public class PointEarning extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "earn_type", nullable = false)
    private PointEarnType earnType;

    @Column(name = "earned_amount", nullable = false)
    private Long earnedAmount;

    @Column(name = "available_amount", nullable = false)
    private Long availableAmount;

    @Column(name = "used_amount", nullable = false)
    private Long usedAmount;

    @Column(name = "canceled_amount", nullable = false)
    private Long canceledAmount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private PointEarning(
            Long transactionId,
            String memberId,
            PointEarnType earnType,
            long earnedAmount,
            LocalDateTime expiresAt
    ) {
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.earnType = earnType;
        this.earnedAmount = earnedAmount;
        this.availableAmount = earnedAmount;
        this.usedAmount = 0L;
        this.canceledAmount = 0L;
        this.expiresAt = expiresAt;
    }

    public static PointEarning create(
            Long transactionId,
            String memberId,
            PointEarnType earnType,
            long earnedAmount,
            LocalDateTime expiresAt
    ) {
        if (transactionId == null) {
            throw new IllegalArgumentException("transactionId는 필수입니다.");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("memberId는 필수입니다.");
        }
        if (earnType == null) {
            throw new IllegalArgumentException("earnType은 필수입니다.");
        }
        if (earnedAmount <= 0L) {
            throw new IllegalArgumentException("적립 금액은 0보다 커야 합니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt은 필수입니다.");
        }
        return new PointEarning(transactionId, memberId, earnType, earnedAmount, expiresAt);
    }

    public void use(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.availableAmount < amount) {
            throw new IllegalStateException("가용 금액보다 큰 금액은 사용할 수 없습니다.");
        }
        this.availableAmount -= amount;
        this.usedAmount += amount;
    }

    public void restore(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("복원 금액은 0보다 커야 합니다.");
        }
        if (this.usedAmount < amount) {
            throw new IllegalStateException("사용된 금액보다 큰 금액은 복원할 수 없습니다.");
        }
        this.usedAmount -= amount;
        this.availableAmount += amount;
    }

    public void cancel() {
        if (this.usedAmount != 0L) {
            throw new IllegalStateException("일부라도 사용된 적립은 취소할 수 없습니다.");
        }
        this.canceledAmount += this.availableAmount;
        this.availableAmount = 0L;
    }

    public boolean isExpired(LocalDateTime now) {
        return !this.expiresAt.isAfter(now);
    }

    public boolean isManual() {
        return this.earnType == PointEarnType.MANUAL;
    }

    public boolean isUsed() {
        return this.usedAmount > 0L;
    }
}
