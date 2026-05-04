package com.musinsa.freepoint.domain.policy;

import com.musinsa.freepoint.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "point_policy",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_policy_policy_key",
                columnNames = "policy_key"
        )
)
public class PointPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_key", nullable = false, unique = true)
    private PointPolicyKey policyKey;

    @Column(name = "policy_value", nullable = false)
    private String policyValue;

    private String description;

    @Column(nullable = false)
    private Boolean enabled;

    private PointPolicy(PointPolicyKey policyKey, String policyValue, String description, Boolean enabled) {
        this.policyKey = policyKey;
        this.policyValue = policyValue;
        this.description = description;
        this.enabled = enabled;
    }

    public static PointPolicy create(PointPolicyKey policyKey, String policyValue, String description) {
        if (policyKey == null) {
            throw new IllegalArgumentException("policyKey는 필수입니다.");
        }
        if (policyValue == null || policyValue.isBlank()) {
            throw new IllegalArgumentException("policyValue는 필수입니다.");
        }
        return new PointPolicy(policyKey, policyValue, description, true);
    }

    public void updateValue(String policyValue) {
        if (policyValue == null || policyValue.isBlank()) {
            throw new IllegalArgumentException("policyValue는 필수입니다.");
        }
        this.policyValue = policyValue;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
