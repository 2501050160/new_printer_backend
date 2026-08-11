package com.saipraveen.login_registration.repository;

import com.saipraveen.login_registration.entity.ManagerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManagerLogRepository extends JpaRepository<ManagerLog, Long> {
    List<ManagerLog> findByCollegeOrderByTimestampDesc(String college);
    List<ManagerLog> findAllByOrderByTimestampDesc();
}
