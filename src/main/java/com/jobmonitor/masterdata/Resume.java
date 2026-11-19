package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** An uploaded resume. Emails get the active one. */
@Entity
@Table(name = "resume")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(nullable = false)
    private byte[] content;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    protected Resume() {
    }

    public Resume(String fileName, String contentType, byte[] content, boolean active) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
        this.sizeBytes = content.length;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getContent() {
        return content;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
