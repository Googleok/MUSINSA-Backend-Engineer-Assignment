package com.musinsa.freepoint.domain.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointUseAllocationRepository extends JpaRepository<PointUseAllocation, Long> {

    List<PointUseAllocation> findByUseTransactionIdOrderByIdAsc(Long useTransactionId);

    @Query("""
            SELECT COALESCE(SUM(a.canceledAmount), 0)
              FROM PointUseAllocation a
             WHERE a.useTransactionId = :useTransactionId
            """)
    long sumCanceledAmountByUseTransactionId(@Param("useTransactionId") Long useTransactionId);
}
