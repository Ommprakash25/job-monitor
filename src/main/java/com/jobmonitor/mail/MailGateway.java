package com.jobmonitor.mail;

/**
 * The one place the app talks to a mail server. During development it talks to the
 * built-in fake server; Gmail plugs in here in phase 3 without changing anything else.
 */
public interface MailGateway {

    MailMode mode();

    /** Signs in for sending and for reading, then signs out again. */
    ConnectionCheck checkConnection(String emailAddress, String secret);

    /** Sends a short test email from the account to itself. */
    void sendTestEmail(String emailAddress, String displayName, String secret);

    /** Sends one email from the account, with its resume attached. */
    /** Returns the Message-ID the server gave the email, which replies refer back to. */
    String send(String fromAddress, String displayName, String secret, OutgoingEmail email);
}
