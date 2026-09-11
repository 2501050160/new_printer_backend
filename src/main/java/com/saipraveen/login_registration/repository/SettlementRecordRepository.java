package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.saipraveen.login_registration.entity.SettlementRecord;
import java.util.List;

@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, Long> {

    List<SettlementRecord> findByCollegeOrderBySettlementDateDesc(String college);

    List<SettlementRecord> findAllByOrderBySettlementDateDesc();

    @Query("SELECT COALESCE(SUM(s.amount), 0.0) FROM SettlementRecord s WHERE LOWER(s.college) = LOWER(:college) AND s.status = 'COMPLETED'")
    Double sumSettledAmountByCollege(@Param("college") String college);
}
