package com.musinsa.freepoint.domain.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    Optional<PointPolicy> findByPolicyKeyAndEnabledTrue(PointPolicyKey policyKey);
}
