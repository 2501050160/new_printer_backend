package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.CampusBlock;

public interface CampusBlockRepository extends JpaRepository<CampusBlock, Long> {
    CampusBlock findByName(String name);
    CampusBlock findByNameIgnoreCase(String name);
    CampusBlock findByServerApiKey(String serverApiKey);
    java.util.List<CampusBlock> findByCollege(String college);
    java.util.List<CampusBlock> findByCollegeIgnoreCase(String college);
}
