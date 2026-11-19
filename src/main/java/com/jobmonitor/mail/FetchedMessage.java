package com.jobmonitor.mail;

import java.time.Instant;
import java.util.List;

/**
 * One message as read from the mailbox. {@code rawIds} holds the raw lines of a bounce
 * (empty for other mail), so the Message-ID of the email that bounced can be found.
 */
public record FetchedMessage(long uid, String messageId, String inReplyTo, String references,
                             String fromAddress, String fromName, String toAddress, String subject,
                             Instant receivedAt, String bodyText, List<String> attachmentNames,
                             List<String> rawIds) {
}
