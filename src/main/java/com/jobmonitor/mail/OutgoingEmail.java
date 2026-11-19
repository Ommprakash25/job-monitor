package com.jobmonitor.mail;

/**
 * One email ready to send. The HTML and plain-text versions go out together, the way
 * Gmail sends a normal message. {@code attachment} may be null.
 */
public record OutgoingEmail(String to, String subject, String textBody, String htmlBody,
                            String attachmentName, String attachmentType, byte[] attachment,
                            String inReplyTo, String references) {
}
