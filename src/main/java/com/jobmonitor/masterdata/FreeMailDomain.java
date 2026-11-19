package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A free-mail domain such as gmail.com. Recruiters on it get the company "Others". */
@Entity
@Table(name = "free_mail_domain")
public class FreeMailDomain {

    @Id
    @Column(length = 253)
    private String domain;

    protected FreeMailDomain() {
    }

    public FreeMailDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }
}
