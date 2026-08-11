package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.CollegeConfig;

public interface CollegeConfigRepository extends JpaRepository<CollegeConfig, Long> {
    CollegeConfig findByCollegeName(String collegeName);
}
