package com.jobmonitor.sending;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A company, taken from a recruiter's email domain. The "Others" company has no domain. */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 253)
    private String domain;

    protected Company() {
    }

    public Company(String name, String domain) {
        this.name = name;
        this.domain = domain;
    }

    public boolean isOthers() {
        return domain == null;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }
}
