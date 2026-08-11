package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.Admin;
import com.saipraveen.login_registration.repository.AdminRepository;

@Service
public class AdminService {

    @Autowired
    private AdminRepository repository;

    @jakarta.annotation.PostConstruct
    public void initDefaultAdmin() {
        try {
            if (repository.count() == 0 || repository.findByUsername("admin") == null) {
                Admin defaultAdmin = new Admin();
                defaultAdmin.setUsername("admin");
                defaultAdmin.setPassword("admin123");
                defaultAdmin.setRole("MAIN_ADMIN");
                defaultAdmin.setCollege("ALL");
                repository.save(defaultAdmin);
            }
        } catch (Exception e) {
            System.err.println("Warning: Default admin initialization deferred: " + e.getMessage());
        }
    }

    public Admin login(
            String username,
            String password
    ) {
        return repository
                .findByUsernameAndPassword(
                        username,
                        password
                );
    }

    public java.util.List<Admin> getAllSubAdmins() {
        return repository.findAll().stream()
                .filter(a -> !"MAIN_ADMIN".equalsIgnoreCase(a.getRole()) && !"admin".equalsIgnoreCase(a.getUsername()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Admin createSubAdmin(Admin subAdmin) {
        if (repository.findByUsername(subAdmin.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }
        if (subAdmin.getRole() == null || (!subAdmin.getRole().equals("MANAGER") && !subAdmin.getRole().equals("SUB_ADMIN"))) {
            subAdmin.setRole("SUB_ADMIN");
        }
        return repository.save(subAdmin);
    }

    public boolean verifyManagerSecret(Long adminId, String secret) {
        Admin admin = repository.findById(adminId).orElse(null);
        if (admin != null && "MANAGER".equals(admin.getRole())) {
            return secret != null && secret.equals(admin.getManagerSecret());
        }
        return false;
    }

    @Transactional
    public void deleteSubAdmin(Long id) {
        repository.deleteById(id);
    }
}