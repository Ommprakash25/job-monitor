package com.jobmonitor.mail;

/** Result of signing in to the mail servers, in words the user can read. */
public record ConnectionCheck(boolean ok, String message) {

    public static ConnectionCheck ok(String message) {
        return new ConnectionCheck(true, message);
    }

    public static ConnectionCheck failed(String message) {
        return new ConnectionCheck(false, message);
    }
}
