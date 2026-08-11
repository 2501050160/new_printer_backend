package com.saipraveen.login_registration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saipraveen.login_registration.entity.FrontendSection;
import java.util.List;

public interface FrontendSectionRepository extends JpaRepository<FrontendSection, Long> {
    List<FrontendSection> findByActiveTrueOrderByDisplayOrderAsc();
    List<FrontendSection> findAllByOrderByDisplayOrderAsc();
}
