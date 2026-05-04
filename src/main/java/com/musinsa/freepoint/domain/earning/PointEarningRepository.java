package com.musinsa.freepoint.domain.earning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointEarningRepository extends JpaRepository<PointEarning, Long> {

    Optional<PointEarning> findByTransactionId(Long transactionId);

    @Query("""
            SELECT e
              FROM PointEarning e
             WHERE e.memberId = :memberId
               AND e.availableAmount > 0
               AND e.expiresAt > :now
             ORDER BY
                CASE WHEN e.earnType = com.musinsa.freepoint.domain.earning.PointEarnType.MANUAL THEN 0 ELSE 1 END ASC,
                e.expiresAt ASC,
                e.id ASC
            """)
    List<PointEarning> findUsableEarnings(
            @Param("memberId") String memberId,
            @Param("now") LocalDateTime now
    );
}
