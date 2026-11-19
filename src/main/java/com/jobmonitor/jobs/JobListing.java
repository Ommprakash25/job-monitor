package com.jobmonitor.jobs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A job posting from a job API. Read only: nothing is applied for automatically. */
@Entity
@Table(name = "job_listing")
public class JobListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 300)
    private String company = "";

    @Column(nullable = false, length = 300)
    private String location = "";

    @Column(nullable = false, columnDefinition = "text")
    private String url = "";

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @Column(name = "found_at", nullable = false)
    private Instant foundAt = Instant.now();

    protected JobListing() {
    }

    public JobListing(String source, String externalId) {
        this.source = source;
        this.externalId = externalId;
    }

    public void update(String title, String company, String location, String url, Instant postedAt, String description) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.postedAt = postedAt;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public String getUrl() {
        return url;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public String getDescription() {
        return description;
    }
}
