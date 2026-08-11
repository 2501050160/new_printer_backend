package com.saipraveen.login_registration.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "campus_blocks")
public class CampusBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = true)
    private String college = "KLU";

    @Column(nullable = true, unique = true)
    private String serverApiKey;

    public CampusBlock() {}

    public CampusBlock(String name) {
        this.name = name;
        this.college = "KLU";
    }

    public CampusBlock(String name, String college) {
        this.name = name;
        this.college = college;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCollege() {
        return college == null ? "KLU" : college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getServerApiKey() {
        return serverApiKey;
    }

    public void setServerApiKey(String serverApiKey) {
        this.serverApiKey = serverApiKey;
    }
}
