package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A sending mailbox. The password or token is stored encrypted, never in plain text. */
@Entity
@Table(name = "mail_account")
public class MailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_address", nullable = false, unique = true, length = 254)
    private String emailAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private AuthType authType = AuthType.APP_PASSWORD;

    @Column(name = "secret_encrypted", columnDefinition = "text")
    private String secretEncrypted;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_check_at")
    private Instant lastCheckAt;

    @Column(name = "last_check_ok")
    private Boolean lastCheckOk;

    @Column(name = "last_check_message", length = 500)
    private String lastCheckMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public boolean hasSecret() {
        return secretEncrypted != null && !secretEncrypted.isBlank();
    }

    public void recordCheck(boolean ok, String message, Instant at) {
        this.lastCheckOk = ok;
        this.lastCheckMessage = message == null || message.length() <= 500 ? message : message.substring(0, 500);
        this.lastCheckAt = at;
    }

    public void clearCheck() {
        this.lastCheckOk = null;
        this.lastCheckMessage = null;
        this.lastCheckAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getSecretEncrypted() {
        return secretEncrypted;
    }

    public void setSecretEncrypted(String secretEncrypted) {
        this.secretEncrypted = secretEncrypted;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastCheckAt() {
        return lastCheckAt;
    }

    public Boolean getLastCheckOk() {
        return lastCheckOk;
    }

    public String getLastCheckMessage() {
        return lastCheckMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
