package com.musinsa.freepoint.domain.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberPointWalletRepository extends JpaRepository<MemberPointWallet, Long> {

    Optional<MemberPointWallet> findByMemberId(String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM MemberPointWallet w WHERE w.memberId = :memberId")
    Optional<MemberPointWallet> findByMemberIdForUpdate(@Param("memberId") String memberId);
}
