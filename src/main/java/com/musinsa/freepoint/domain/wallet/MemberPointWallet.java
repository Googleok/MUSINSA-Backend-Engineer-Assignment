package com.musinsa.freepoint.domain.wallet;

import com.musinsa.freepoint.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_point_wallet",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_point_wallet_member_id",
                columnNames = "member_id"
        )
)
public class MemberPointWallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private String memberId;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    private MemberPointWallet(String memberId) {
        this.memberId = memberId;
        this.balance = 0L;
    }

    public static MemberPointWallet create(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("memberId는 필수입니다.");
        }
        return new MemberPointWallet(memberId);
    }

    public void increaseBalance(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("증가 금액은 0보다 커야 합니다.");
        }
        this.balance += amount;
    }

    public void decreaseBalance(long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException("감소 금액은 0보다 커야 합니다.");
        }
        if (this.balance < amount) {
            throw new IllegalStateException("잔액보다 큰 금액을 차감할 수 없습니다.");
        }
        this.balance -= amount;
    }
}
