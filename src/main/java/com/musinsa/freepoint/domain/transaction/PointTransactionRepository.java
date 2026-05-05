package com.musinsa.freepoint.domain.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    Optional<PointTransaction> findByPointKey(String pointKey);

    List<PointTransaction> findByMemberIdOrderByIdAsc(String memberId);
}
