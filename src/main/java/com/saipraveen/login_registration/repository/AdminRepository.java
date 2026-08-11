package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.saipraveen.login_registration.entity.Admin;

public interface AdminRepository
        extends JpaRepository<Admin, Long> {

    Admin findByUsernameAndPassword(
            String username,
            String password
    );

    Admin findByUsername(String username);
}