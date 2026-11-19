package com.jobmonitor.mail;

/** Where to send (SMTP) and read (IMAP) mail. */
public record MailServerSettings(
        String smtpHost, int smtpPort, boolean smtpStartTls, boolean smtpSsl,
        String imapHost, int imapPort, boolean imapSsl) {

    /** The built-in fake mail server: plain connections on 127.0.0.1 only. */
    public static MailServerSettings fake(int smtpPort, int imapPort) {
        return new MailServerSettings("127.0.0.1", smtpPort, false, false, "127.0.0.1", imapPort, false);
    }

    /** Gmail with an app password (the phase 3 fallback if the Gmail API trial doesn't work out). */
    public static MailServerSettings gmail() {
        return new MailServerSettings("smtp.gmail.com", 587, true, false, "imap.gmail.com", 993, true);
    }
}
