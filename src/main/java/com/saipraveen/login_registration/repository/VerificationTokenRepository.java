package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.saipraveen.login_registration.entity.VerificationToken;
import com.saipraveen.login_registration.entity.User;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    VerificationToken findByToken(String token);
    void deleteByUser(User user);
}
