package com.jobmonitor.sending;

/** One row as it will be handled, with what the app decided and whether it will be queued. */
public record PreviewRow(int line, String email, String name, String company, String role, String status,
                         boolean ready) {
}
