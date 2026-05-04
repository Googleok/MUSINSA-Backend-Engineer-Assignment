package com.musinsa.freepoint.domain.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "point_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_transaction_point_key",
                columnNames = "point_key"
        )
)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_key", nullable = false, unique = true)
    private String pointKey;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointTransactionType transactionType;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "order_no")
    private String orderNo;

    @Column(name = "original_transaction_id")
    private Long originalTransactionId;

    private String reason;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PointTransaction(
            String pointKey,
            String memberId,
            PointTransactionType transactionType,
            long amount,
            String orderNo,
            Long originalTransactionId,
            String reason,
            String createdBy
    ) {
        this.pointKey = pointKey;
        this.memberId = memberId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.orderNo = orderNo;
        this.originalTransactionId = originalTransactionId;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public static PointTransaction earn(
            String pointKey,
            String memberId,
            long amount,
            String reason,
            String createdBy
    ) {
        return new PointTransaction(
                pointKey, memberId, PointTransactionType.EARN, amount,
                null, null, reason, createdBy
        );
    }

    public static PointTransaction earnCancel(
            String pointKey,
            String memberId,
            long amount,
            Long originalTransactionId,
            String reason,
            String createdBy
    ) {
        return new PointTransaction(
                pointKey, memberId, PointTransactionType.EARN_CANCEL, amount,
                null, originalTransactionId, reason, createdBy
        );
    }

    public static PointTransaction use(
            String pointKey,
            String memberId,
            long amount,
            String orderNo,
            String reason,
            String createdBy
    ) {
        return new PointTransaction(
                pointKey, memberId, PointTransactionType.USE, amount,
                orderNo, null, reason, createdBy
        );
    }

    public static PointTransaction useCancel(
            String pointKey,
            String memberId,
            long amount,
            String orderNo,
            Long originalTransactionId,
            String reason,
            String createdBy
    ) {
        return new PointTransaction(
                pointKey, memberId, PointTransactionType.USE_CANCEL, amount,
                orderNo, originalTransactionId, reason, createdBy
        );
    }
}
