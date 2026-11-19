package com.jobmonitor.mail;

import com.jobmonitor.config.JobMonitorProperties;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.springframework.stereotype.Component;

/**
 * Reads new messages from a mail folder over IMAP, read-only: nothing is marked, moved or deleted.
 */
@Component
public class ImapReader {

    private static final int MAX_BODY_CHARS = 100_000;

    /** One folder's new messages, and the folder's UID validity so the next read knows where to start. */
    public record Batch(long uidValidity, List<FetchedMessage> messages) {
    }

    private final JobMonitorProperties properties;

    public ImapReader(JobMonitorProperties properties) {
        this.properties = properties;
    }

    /** Messages in {@code folderName} with a UID above {@code afterUid}. */
    public Batch fetchAfter(String address, String secret, String folderName, long afterUid,
                            long knownUidValidity) throws MessagingException, IOException {
        MailServerSettings servers = settings();
        Properties p = new Properties();
        p.setProperty("mail.imap.ssl.enable", Boolean.toString(servers.imapSsl()));
        p.setProperty("mail.imap.connectiontimeout", "15000");
        p.setProperty("mail.imap.timeout", "60000");
        Session session = Session.getInstance(p);

        try (Store store = session.getStore("imap")) {
            store.connect(servers.imapHost(), servers.imapPort(), address, secret);
            Folder folder = store.getFolder(folderName);
            if (!folder.exists()) {
                return new Batch(0, List.of());
            }
            folder.open(Folder.READ_ONLY);
            try {
                UIDFolder uids = (UIDFolder) folder;
                long validity = uids.getUIDValidity();
                boolean same = validity == knownUidValidity;
                long from = same ? afterUid + 1 : 1;
                Message[] found = uids.getMessagesByUID(from, UIDFolder.LASTUID);
                if (found == null || found.length == 0) {
                    return new Batch(validity, List.of());
                }
                FetchProfile profile = new FetchProfile();
                profile.add(FetchProfile.Item.ENVELOPE);
                profile.add("Message-ID");
                profile.add("In-Reply-To");
                profile.add("References");
                folder.fetch(found, profile);

                List<FetchedMessage> out = new ArrayList<>();
                for (Message message : found) {
                    long uid = uids.getUID(message);
                    if (same && uid <= afterUid) {
                        continue;
                    }
                    out.add(toFetched(uid, (MimeMessage) message));
                }
                return new Batch(validity, out);
            } finally {
                folder.close(false);
            }
        }
    }

    private MailServerSettings settings() {
        JobMonitorProperties.Mail mail = properties.mail();
        return mail.mode() == MailMode.GMAIL
                ? MailServerSettings.gmail()
                : MailServerSettings.fake(mail.fakeSmtpPort(), mail.fakeImapPort());
    }

    private static FetchedMessage toFetched(long uid, MimeMessage m) throws MessagingException, IOException {
        InternetAddress from = firstAddress(m.getFrom());
        String fromAddress = from == null ? "" : from.getAddress().toLowerCase(Locale.ROOT);
        String fromName = from == null || from.getPersonal() == null ? "" : from.getPersonal();
        StringBuilder to = new StringBuilder();
        Address[] recipients = m.getRecipients(Message.RecipientType.TO);
        if (recipients != null) {
            for (Address a : recipients) {
                if (to.length() > 0) {
                    to.append(", ");
                }
                to.append(a.toString());
            }
        }
        Instant received = m.getSentDate() != null ? m.getSentDate().toInstant()
                : m.getReceivedDate() != null ? m.getReceivedDate().toInstant() : Instant.now();

        StringBuilder body = new StringBuilder();
        List<String> attachments = new ArrayList<>();
        collect(m, body, attachments);

        List<String> rawIds = List.of();
        if (fromAddress.startsWith("mailer-daemon") || fromAddress.startsWith("postmaster")) {
            // A bounce quotes the original email, so its Message-ID is somewhere in the raw text.
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            m.writeTo(raw);
            rawIds = List.of(raw.toString(java.nio.charset.StandardCharsets.UTF_8).split("\\R"));
        }
        return new FetchedMessage(uid, firstHeader(m, "Message-ID"), firstHeader(m, "In-Reply-To"),
                firstHeader(m, "References"), fromAddress, fromName, to.toString(), nullToEmpty(m.getSubject()),
                received, truncate(body.toString()), attachments, rawIds);
    }

    private static void collect(Part part, StringBuilder body, List<String> attachments)
            throws MessagingException, IOException {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collect(multipart.getBodyPart(i), body, attachments);
            }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            attachments.add(part.getFileName() == null ? "attachment" : part.getFileName());
        } else if (part.isMimeType("text/plain") && body.length() < MAX_BODY_CHARS) {
            Object content = part.getContent();
            if (content instanceof String text) {
                body.append(text).append('\n');
            }
        }
    }

    private static InternetAddress firstAddress(Address[] addresses) {
        return addresses != null && addresses.length > 0 && addresses[0] instanceof InternetAddress ia ? ia : null;
    }

    private static String firstHeader(MimeMessage m, String name) throws MessagingException {
        String[] values = m.getHeader(name);
        return values == null || values.length == 0 ? "" : values[0];
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String text) {
        return text.length() <= MAX_BODY_CHARS ? text : text.substring(0, MAX_BODY_CHARS);
    }
}
