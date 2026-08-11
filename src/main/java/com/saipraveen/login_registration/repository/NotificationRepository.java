package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.Notification;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCollegeIn(List<String> colleges);
}
