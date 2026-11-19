package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The person applying. There is always exactly one row. */
@Entity
@Table(name = "profile")
public class Profile {

    public static final int SINGLE_ID = 1;

    @Id
    private Integer id = SINGLE_ID;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName = "";

    @Column(nullable = false, length = 40)
    private String phone = "";

    @Column(nullable = false, columnDefinition = "text")
    private String signature = "";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Integer getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
