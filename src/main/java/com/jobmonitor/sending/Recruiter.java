package com.jobmonitor.sending;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A person we can email. Their name and company are kept separately. */
@Entity
@Table(name = "recruiter")
public class Recruiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 120)
    private String name = "";

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Where the recruiter came from: PASTED, COMPOSE, or IMPORTED (the old lists). */
    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "do_not_contact", nullable = false)
    private boolean doNotContact;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Recruiter() {
    }

    public Recruiter(String email, String name, Company company, String source) {
        this.email = email;
        this.name = name == null ? "" : name;
        this.company = company;
        this.source = source;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Company getCompany() {
        return company;
    }

    public String getSource() {
        return source;
    }

    public boolean isDoNotContact() {
        return doNotContact;
    }

    public void setDoNotContact(boolean doNotContact) {
        this.doNotContact = doNotContact;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
