package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import com.saipraveen.login_registration.entity.UserRewardClaim;

public interface UserRewardClaimRepository extends JpaRepository<UserRewardClaim, Long> {
    boolean existsByUserIdAndRewardId(Long userId, Long rewardId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserRewardClaim u WHERE u.rewardId = :rewardId")
    void deleteByRewardId(@Param("rewardId") Long rewardId);
}

