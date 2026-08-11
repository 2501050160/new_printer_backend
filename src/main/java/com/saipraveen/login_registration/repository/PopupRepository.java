package com.saipraveen.login_registration.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.saipraveen.login_registration.entity.Popup;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long> {
    List<Popup> findByActiveTrue();
    List<Popup> findByActiveTrueAndTargetPage(String targetPage);
}
