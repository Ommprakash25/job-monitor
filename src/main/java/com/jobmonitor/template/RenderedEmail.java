package com.jobmonitor.template;

import java.util.List;

/**
 * A template filled in for one recruiter. Emails go out with both versions, the way
 * Gmail sends a normal message: HTML, plus plain text for mail apps that prefer it.
 */
public record RenderedEmail(String subject, String textBody, String htmlBody, List<String> warnings) {

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
