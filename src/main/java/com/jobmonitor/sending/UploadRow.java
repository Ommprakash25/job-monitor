package com.jobmonitor.sending;

/** One row of an uploaded Excel or CSV file. {@code line} is the row number in the file, for messages. */
public record UploadRow(int line, String email, String name, String role) {
}
