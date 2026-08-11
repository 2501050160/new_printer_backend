package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
