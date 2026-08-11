package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendOtpEmail(String toEmail, String otp, String subject, String bodyTemplate) {
        System.out.println("==================================================");
        System.out.println("OTP FOR " + toEmail + " IS: " + otp);
        System.out.println("==================================================");
        
        if (mailSender == null || mailUsername == null || mailUsername.trim().isEmpty()) {
            System.out.println("SMTP Mail Sender username is empty. Skipping email delivery (console fallback only).");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(String.format(bodyTemplate, otp));
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}
