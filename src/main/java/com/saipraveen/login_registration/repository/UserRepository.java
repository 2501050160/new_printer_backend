package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    User findByReferralCode(String referralCode);
}