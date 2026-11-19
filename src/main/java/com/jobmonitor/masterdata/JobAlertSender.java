package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A job portal that sends alerts: a domain such as naukri.com, or a full address. */
@Entity
@Table(name = "job_alert_sender")
public class JobAlertSender {

    @Id
    @Column(length = 254)
    private String pattern;

    protected JobAlertSender() {
    }

    public JobAlertSender(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }
}
