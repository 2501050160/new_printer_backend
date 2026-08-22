package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationService verificationService;

    @jakarta.annotation.PostConstruct
    public void initDefaultTesterUser() {
        try {
            if (repository.findByEmail("tester@cloudprint.com") == null) {
                User testUser = new User();
                testUser.setName("Tester User");
                testUser.setEmail("tester@cloudprint.com");
                testUser.setPassword("tester123");
                testUser.setWalletBalance(100.0);
                testUser.setEmailVerified(true);
                testUser.setBlocked(false);
                testUser.setReferralCode("TEST88");
                repository.save(testUser);
                System.out.println("✅ Default tester user initialized (tester@cloudprint.com / tester123)");
            }
        } catch (Exception e) {
            System.err.println("Warning: Default tester user init deferred: " + e.getMessage());
        }
    }

    public User registerUser(User user) {
        if (repository.findByEmail(user.getEmail()) != null) {
            throw new RuntimeException("Username is already registered.");
        }
        if (user.getReferralCode() == null || user.getReferralCode().trim().isEmpty()) {
            String code;
            do {
                code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
            } while (repository.findByReferralCode(code) != null);
            user.setReferralCode(code);
        }
        user.setBlocked(false);
        user.setEmailVerified(true);
        
        User savedUser = repository.save(user);
        
        return savedUser;
    }

    public User loginUser(String identifier, String password) {
        User user = repository.findByEmail(identifier);
        if (user == null && identifier != null) {
            String cleanPhone = identifier.replaceAll("[^0-9]", "");
            if (!cleanPhone.isEmpty()) {
                String waEmail = "wa_" + cleanPhone + "@whatsapp.cloudprint";
                user = repository.findByEmail(waEmail);
                if (user == null && cleanPhone.length() >= 7) {
                    user = repository.findByReferralCode("WA_" + cleanPhone);
                }
            }
        }
        if (user != null) {
            if (Boolean.TRUE.equals(user.getBlocked())) {
                throw new RuntimeException("This account is blocked by Admin.");
            }
            if ("WA_BOT_USER_NOPASS".equals(user.getPassword()) || user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }
        user.setEmailVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        repository.save(user);
        return true;
    }

    @Transactional
    public boolean resendOtp(String email) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getEmailVerified() != null && user.getEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusHours(24));
        repository.save(user);
        
        emailService.sendOtpEmail(
            user.getEmail(),
            otp,
            "Cloud Print - Verify your email",
            "Please use the following 6-digit OTP code to verify your email address:\n\n%s\n\nThis code is valid for 24 hours."
        );
        
        return true;
    }

    @Transactional
    public boolean forgotPassword(String email) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User with this email does not exist.");
        }
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));
        repository.save(user);
        
        emailService.sendOtpEmail(
            user.getEmail(),
            otp,
            "Cloud Print - Password Reset Verification Code",
            "You requested a password reset. Your verification code is: %s\nThis code will expire in 5 minutes."
        );
        
        return true;
    }

    @Transactional
    public boolean resetPassword(String email, String otp, String newPassword) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }
        user.setPassword(newPassword);
        user.setOtp(null);
        user.setOtpExpiry(null);
        repository.save(user);
        return true;
    }

    @Transactional
    public void verifyUserEmail(User user) {
        user.setEmailVerified(true);
        repository.save(user);
    }

    public User findByEmail(String email) {
        if (email == null) return null;
        return repository.findByEmail(email.trim());
    }

    public User getUserById(Long userId) {

        return repository.findById(userId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );
    }

    public Double getWalletBalance(Long userId) {

        return getUserById(userId).getWalletBalance();
    }

    @Transactional
    public User creditWallet(Long userId, Double amount) {

        User user = getUserById(userId);

        double current =
                user.getWalletBalance() == null
                        ? 0.0
                        : user.getWalletBalance();

        user.setWalletBalance(current + amount);

        return repository.saveAndFlush(user);
    }

    @Transactional
    public User debitWallet(Long userId, Double amount) {

        User user = getUserById(userId);

        double current =
                user.getWalletBalance() == null
                        ? 0.0
                        : user.getWalletBalance();

        if (current < amount) {
            throw new RuntimeException(
                    "Insufficient wallet balance"
            );
        }

        user.setWalletBalance(current - amount);

        return repository.save(user);
    }

    public java.util.List<User> getAllUsers() {
        return repository.findAll();
    }

    @Transactional
    public User toggleBlockUser(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBlocked(!user.getBlocked());
        return repository.save(user);
    }

    public boolean userExists(Long id) {
        if (id == null) return false;
        return repository.existsById(id);
    }

    @Transactional
    public void deleteUser(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void updateUserPassword(String email, String newPassword) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        user.setPassword(newPassword);
        repository.save(user);
    }

    @Transactional
    public void updateUserPasswordAndCollege(String email, String newPassword, String college) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        user.setPassword(newPassword);
        if (college != null && !college.trim().isEmpty()) {
            user.setCollege(college.trim());
        }
        repository.save(user);
    }

    @Transactional
    public void updateUserCollege(String email, String college) {
        User user = repository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (college != null && !college.trim().isEmpty()) {
            user.setCollege(college.trim());
        }
        repository.save(user);
    }

    @Transactional
    public User updateUserCollegeById(Long id, String college) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (college != null && !college.trim().isEmpty()) {
            user.setCollege(college.trim().toUpperCase());
        }
        return repository.save(user);
    }
}