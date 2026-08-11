package com.saipraveen.login_registration.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.entity.VerificationToken;
import com.saipraveen.login_registration.repository.VerificationTokenRepository;

@Service
public class VerificationService {

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Transactional
    public String createVerificationToken(User user) {
        // Delete any existing token for this user to avoid duplication
        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryTime(LocalDateTime.now().plusHours(24));

        tokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public User validateToken(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        if (verificationToken == null) {
            throw new RuntimeException("Invalid verification token.");
        }

        if (verificationToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken);
            throw new RuntimeException("Verification token has expired.");
        }

        User user = verificationToken.getUser();
        tokenRepository.delete(verificationToken);
        return user;
    }
}
