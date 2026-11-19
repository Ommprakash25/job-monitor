package com.jobmonitor.masterdata;

import java.time.Instant;

/** A resume without its file content, for lists. */
public record ResumeSummary(Long id, String fileName, String contentType, int sizeBytes, boolean active,
                            Instant uploadedAt) {

    public String readableSize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return Math.round(sizeBytes / 1024.0) + " KB";
        }
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
