package com.jobmonitor.mail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.activation.DataHandler;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

/** Talks SMTP and IMAP through Jakarta Mail. Used for the fake server now, and for Gmail's app-password fallback. */
public class JavaMailGateway implements MailGateway {

    private final MailMode mode;
    private final MailServerSettings servers;
    private final FakeMailServer fakeServer;

    /**
     * @param fakeServer the fake server to create mailboxes on, or {@code null} when talking to a real server
     */
    public JavaMailGateway(MailMode mode, MailServerSettings servers, FakeMailServer fakeServer) {
        this.mode = mode;
        this.servers = servers;
        this.fakeServer = fakeServer;
    }

    @Override
    public MailMode mode() {
        return mode;
    }

    @Override
    public ConnectionCheck checkConnection(String emailAddress, String secret) {
        prepareFakeMailbox(emailAddress, secret);
        Session session = session(emailAddress);
        String server = serverName();

        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(servers.smtpHost(), servers.smtpPort(), emailAddress, secret);
        } catch (AuthenticationFailedException e) {
            return ConnectionCheck.failed("Sending: " + server + " rejected the address or password.");
        } catch (MessagingException e) {
            return ConnectionCheck.failed("Sending: couldn't reach " + server + " (" + e.getMessage() + ").");
        }

        try (Store store = session.getStore("imap")) {
            store.connect(servers.imapHost(), servers.imapPort(), emailAddress, secret);
        } catch (AuthenticationFailedException e) {
            return ConnectionCheck.failed("Reading: " + server + " rejected the address or password.");
        } catch (MessagingException e) {
            return ConnectionCheck.failed("Reading: couldn't reach " + server + " (" + e.getMessage() + ").");
        }

        return ConnectionCheck.ok("Signed in to " + server + " for sending and reading.");
    }

    @Override
    public void sendTestEmail(String emailAddress, String displayName, String secret) {
        prepareFakeMailbox(emailAddress, secret);
        Session session = session(emailAddress);
        try {
            MimeMessage message = new MimeMessage(session);
            String name = displayName == null || displayName.isBlank() ? null : displayName.trim();
            message.setFrom(new InternetAddress(emailAddress, name, "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(emailAddress));
            message.setSubject("job-monitor test email", "UTF-8");
            message.setText("This is a test email from job-monitor. If you can read it, sending works.", "UTF-8");
            message.setSentDate(new Date());
            Transport.send(message, emailAddress, secret);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailGatewayException("The test email couldn't be sent: " + e.getMessage(), e);
        }
    }

    @Override
    public String send(String fromAddress, String displayName, String secret, OutgoingEmail email) {
        prepareFakeMailbox(fromAddress, secret);
        Session session = session(fromAddress);
        try {
            MimeMessage message = new MimeMessage(session);
            String name = displayName == null || displayName.isBlank() ? null : displayName.trim();
            message.setFrom(new InternetAddress(fromAddress, name, "UTF-8"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email.to()));
            message.setSubject(email.subject(), "UTF-8");
            if (email.inReplyTo() != null && !email.inReplyTo().isBlank()) {
                // A follow-up: Gmail files it in the same conversation as the original.
                message.setHeader("In-Reply-To", email.inReplyTo());
                String references = email.references() == null || email.references().isBlank()
                        ? email.inReplyTo() : email.references();
                message.setHeader("References", references);
            }
            message.setSentDate(new Date());

            MimeMultipart alternative = new MimeMultipart("alternative");
            MimeBodyPart text = new MimeBodyPart();
            text.setText(email.textBody(), "UTF-8");
            alternative.addBodyPart(text);
            MimeBodyPart html = new MimeBodyPart();
            html.setContent(email.htmlBody(), "text/html; charset=UTF-8");
            alternative.addBodyPart(html);
            MimeBodyPart bodies = new MimeBodyPart();
            bodies.setContent(alternative);

            MimeMultipart mixed = new MimeMultipart("mixed");
            mixed.addBodyPart(bodies);
            if (email.attachment() != null) {
                MimeBodyPart file = new MimeBodyPart();
                file.setDataHandler(new DataHandler(new ByteArrayDataSource(email.attachment(), email.attachmentType())));
                file.setFileName(email.attachmentName());
                mixed.addBodyPart(file);
            }
            message.setContent(mixed);
            message.saveChanges();
            Transport.send(message, fromAddress, secret);
            return message.getMessageID();
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailGatewayException("The email to " + email.to() + " couldn't be sent: " + e.getMessage(), e);
        }
    }

    private String serverName() {
        return mode == MailMode.FAKE ? "the built-in fake mail server" : "Gmail";
    }

    private void prepareFakeMailbox(String emailAddress, String secret) {
        if (fakeServer != null) {
            fakeServer.ensureUser(emailAddress, secret);
        }
    }

    private Session session(String fromAddress) {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", servers.smtpHost());
        p.setProperty("mail.smtp.port", Integer.toString(servers.smtpPort()));
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.smtp.starttls.enable", Boolean.toString(servers.smtpStartTls()));
        p.setProperty("mail.smtp.starttls.required", Boolean.toString(servers.smtpStartTls()));
        p.setProperty("mail.smtp.ssl.enable", Boolean.toString(servers.smtpSsl()));
        // Introduce ourselves as "localhost", so the computer's name stays out of the Received header.
        p.setProperty("mail.smtp.localhost", "localhost");
        // Message-IDs then end with the sender's domain (…@gmail.com) instead of the computer's name.
        p.setProperty("mail.from", fromAddress);
        p.setProperty("mail.smtp.connectiontimeout", "10000");
        p.setProperty("mail.smtp.timeout", "20000");
        p.setProperty("mail.smtp.writetimeout", "20000");
        p.setProperty("mail.imap.ssl.enable", Boolean.toString(servers.imapSsl()));
        p.setProperty("mail.imap.connectiontimeout", "10000");
        p.setProperty("mail.imap.timeout", "20000");
        return Session.getInstance(p);
    }
}
