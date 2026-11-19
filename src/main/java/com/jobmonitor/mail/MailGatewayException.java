package com.jobmonitor.mail;

public class MailGatewayException extends RuntimeException {

    public MailGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
