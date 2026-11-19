package com.jobmonitor.inbox;

import com.jobmonitor.mail.FetchedMessage;
import com.jobmonitor.mail.ImapReader;
import com.jobmonitor.masterdata.JobAlertSender;
import com.jobmonitor.masterdata.JobAlertSenderRepository;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountRepository;
import com.jobmonitor.masterdata.SendingSettingsService;
import com.jobmonitor.security.SecretCipher;
import com.jobmonitor.sending.Company;
import com.jobmonitor.sending.CompanyRepository;
import com.jobmonitor.sending.EmailLog;
import com.jobmonitor.sending.EmailLogRepository;
import com.jobmonitor.sending.Recruiter;
import com.jobmonitor.sending.RecruiterRepository;
import com.jobmonitor.sending.SendStatus;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the Inbox, Spam and Sent Mail folders, and links each message to what it's about:
 * a reply to an email we sent, a recruiter we know, or a company we emailed.
 * Bounces mark the recruiter as do-not-contact. Reads only; nothing is changed in Gmail.
 */
@Service
@Transactional
public class InboxSyncService {

    public record SyncResult(int added, int linked, int bounces) {
    }

    /** Gmail's folder name, and the label the app shows for it. */
    private static final Map<String, String> FOLDERS = new LinkedHashMap<>();
    static {
        FOLDERS.put("INBOX", "INBOX");
        FOLDERS.put("[Gmail]/Spam", "SPAM");
        FOLDERS.put("[Gmail]/Sent Mail", "SENT");
    }

    private static final Pattern MESSAGE_ID = Pattern.compile("<[^<>\\s]+@[^<>\\s]+>");
    private static final Pattern BOUNCE_SUBJECT = Pattern.compile(
            "(?i).*(delivery status notification|undelivered mail|delivery (has )?failed|mail delivery failed|failure notice|returned mail).*");

    private final InboxMessageRepository messages;
    private final InboxSyncStateRepository states;
    private final EmailLogRepository logs;
    private final RecruiterRepository recruiters;
    private final CompanyRepository companies;
    private final JobAlertSenderRepository jobAlerts;
    private final MailAccountRepository accounts;
    private final SecretCipher cipher;
    private final ImapReader imap;
    private final SendingSettingsService settings;

    public InboxSyncService(InboxMessageRepository messages, InboxSyncStateRepository states,
                            EmailLogRepository logs, RecruiterRepository recruiters, CompanyRepository companies,
                            JobAlertSenderRepository jobAlerts, MailAccountRepository accounts, SecretCipher cipher,
                            ImapReader imap, SendingSettingsService settings) {
        this.messages = messages;
        this.states = states;
        this.logs = logs;
        this.recruiters = recruiters;
        this.companies = companies;
        this.jobAlerts = jobAlerts;
        this.accounts = accounts;
        this.cipher = cipher;
        this.imap = imap;
        this.settings = settings;
    }

    public boolean isConfigured() {
        return accounts.findFirstByActiveTrue().map(MailAccount::hasSecret).orElse(false);
    }

    public SyncResult sync() {
        MailAccount account = accounts.findFirstByActiveTrue().filter(MailAccount::hasSecret)
                .orElseThrow(() -> new IllegalStateException("Connect the Gmail account first, in Master data."));
        String secret = cipher.decrypt(account.getSecretEncrypted());
        String mine = account.getEmailAddress();

        int added = 0;
        int linked = 0;
        int bounces = 0;
        for (Map.Entry<String, String> folder : FOLDERS.entrySet()) {
            String gmailName = folder.getKey();
            String label = folder.getValue();
            InboxSyncState state = states.findById(gmailName).orElseGet(() -> new InboxSyncState(gmailName));
            ImapReader.Batch batch;
            try {
                batch = imap.fetchAfter(mine, secret, gmailName, state.getLastUid(), state.getUidValidity());
            } catch (MessagingException | IOException e) {
                throw new IllegalStateException("Couldn't read " + gmailName + ": " + e.getMessage(), e);
            }
            long maxUid = batch.uidValidity() == state.getUidValidity() ? state.getLastUid() : 0;
            for (FetchedMessage fetched : batch.messages()) {
                maxUid = Math.max(maxUid, fetched.uid());
                if (isDuplicate(label, fetched)) {
                    continue;
                }
                InboxMessage saved = store(label, fetched);
                added++;
                if (!"SENT".equals(label)) {
                    Outcome outcome = classifyAndMatch(saved, fetched, mine);
                    if (outcome.linked()) {
                        linked++;
                    }
                    if (outcome.bounce()) {
                        bounces++;
                    }
                }
            }
            state.setUidValidity(batch.uidValidity());
            state.setLastUid(maxUid);
            states.save(state);
        }
        return new SyncResult(added, linked, bounces);
    }

    /** Emails sent with no reply yet, older than the follow-up delay in Sending settings. */
    @Transactional(readOnly = true)
    public List<EmailLog> needsFollowUp() {
        Instant before = Instant.now().minus(Duration.ofDays(settings.get().getFollowUpAfterDays()));
        return logs.findTop100ByStatusAndSentAtBeforeAndRepliedAtIsNullAndBouncedAtIsNullOrderBySentAtAsc(
                SendStatus.SENT, before);
    }

    private boolean isDuplicate(String label, FetchedMessage fetched) {
        if (!fetched.messageId().isBlank()) {
            return messages.existsByMessageId(fetched.messageId());
        }
        return messages.existsByFolderAndImapUid(label, fetched.uid());
    }

    private InboxMessage store(String label, FetchedMessage fetched) {
        InboxMessage m = new InboxMessage(label, fetched.uid());
        m.setMessageId(fetched.messageId().isBlank() ? null : fetched.messageId());
        m.setFromAddress(truncate(fetched.fromAddress(), 254));
        m.setFromName(truncate(fetched.fromName(), 255));
        m.setToAddress(truncate(fetched.toAddress(), 998));
        m.setSubject(truncate(fetched.subject(), 998));
        m.setReceivedAt(fetched.receivedAt());
        m.setBodyText(fetched.bodyText());
        m.setAttachmentNames(String.join("\n", fetched.attachmentNames()));
        m.setCategory("JOB_CONVERSATION");
        return messages.save(m);
    }

    private record Outcome(boolean linked, boolean bounce) {
    }

    private Outcome classifyAndMatch(InboxMessage m, FetchedMessage fetched, String mine) {
        String domain = domainOf(fetched.fromAddress());
        boolean bounce = isBounce(fetched.fromAddress(), fetched.subject());
        if (bounce) {
            m.setCategory("BOUNCE");
            return new Outcome(handleBounce(m, fetched), true);
        }
        m.setCategory(isJobAlert(fetched.fromAddress(), domain) ? "JOB_ALERT" : "JOB_CONVERSATION");

        Set<String> ids = messageIds(fetched.inReplyTo() + " " + fetched.references());
        Optional<EmailLog> reply = ids.stream().map(logs::findFirstByMessageId)
                .flatMap(Optional::stream).findFirst();
        if (reply.isPresent()) {
            EmailLog sent = reply.get();
            Recruiter recruiter = sent.getRecruiter();
            m.link("REPLY", recruiter, recruiter.getCompany(), sent);
            if (!fetched.fromAddress().equalsIgnoreCase(mine) && sent.getRepliedAt() == null) {
                sent.setRepliedAt(fetched.receivedAt());
            }
            return new Outcome(true, false);
        }

        Optional<Recruiter> known = recruiters.findByEmail(fetched.fromAddress());
        if (known.isPresent()) {
            m.link("RECRUITER", known.get(), known.get().getCompany(), null);
            return new Outcome(true, false);
        }
        Optional<Company> company = domain.isEmpty() ? Optional.empty() : companies.findByDomain(domain);
        if (company.isPresent()) {
            m.link("COMPANY", null, company.get(), null);
            return new Outcome(true, false);
        }
        return new Outcome(false, false);
    }

    /** A bounce quotes the email that failed. Find that email, mark it bounced, and stop emailing its recruiter. */
    private boolean handleBounce(InboxMessage m, FetchedMessage fetched) {
        Set<String> ids = messageIds(String.join(" ", fetched.rawIds()) + " " + fetched.inReplyTo()
                + " " + fetched.references());
        Optional<EmailLog> failed = ids.stream().map(logs::findFirstByMessageId)
                .flatMap(Optional::stream).findFirst();
        if (failed.isEmpty()) {
            return false;
        }
        EmailLog sent = failed.get();
        Recruiter recruiter = sent.getRecruiter();
        m.link("BOUNCE", recruiter, recruiter.getCompany(), sent);
        if (sent.getBouncedAt() == null) {
            sent.setBouncedAt(fetched.receivedAt());
        }
        recruiter.setDoNotContact(true);
        return true;
    }

    private boolean isJobAlert(String fromAddress, String domain) {
        for (JobAlertSender sender : jobAlerts.findAll()) {
            String pattern = sender.getPattern();
            if (pattern.contains("@") ? pattern.equalsIgnoreCase(fromAddress)
                    : domain.equals(pattern) || domain.endsWith("." + pattern)) {
                return true;
            }
        }
        return false;
    }

    /** Every Message-ID in a block of text, with its angle brackets, as Gmail writes them. */
    public static Set<String> messageIds(String text) {
        Set<String> ids = new java.util.LinkedHashSet<>();
        Matcher matcher = MESSAGE_ID.matcher(text == null ? "" : text);
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return ids;
    }

    /** Bounces come from mailer-daemon or postmaster, or have a delivery-failure subject. */
    public static boolean isBounce(String fromAddress, String subject) {
        String from = fromAddress == null ? "" : fromAddress.toLowerCase(Locale.ROOT);
        return from.startsWith("mailer-daemon") || from.startsWith("postmaster")
                || BOUNCE_SUBJECT.matcher(subject == null ? "" : subject).matches();
    }

    static String domainOf(String address) {
        int at = address.indexOf('@');
        return at < 0 ? "" : address.substring(at + 1).toLowerCase(Locale.ROOT);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
