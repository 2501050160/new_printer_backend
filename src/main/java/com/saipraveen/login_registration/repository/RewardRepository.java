package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.Reward;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    Reward findByClaimCode(String claimCode);
}
