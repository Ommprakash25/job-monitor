package com.jobmonitor.inbox;

import com.jobmonitor.sending.Company;
import com.jobmonitor.sending.EmailLog;
import com.jobmonitor.sending.Recruiter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A message from the Inbox, Spam or Sent Mail folder, with what it was matched to. */
@Entity
@Table(name = "inbox_message")
public class InboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** INBOX, SPAM or SENT. */
    @Column(nullable = false, length = 10)
    private String folder;

    @Column(name = "imap_uid", nullable = false)
    private long imapUid;

    @Column(name = "message_id", length = 998)
    private String messageId;

    @Column(name = "from_address", nullable = false, length = 254)
    private String fromAddress = "";

    @Column(name = "from_name", nullable = false, length = 255)
    private String fromName = "";

    @Column(name = "to_address", nullable = false, length = 998)
    private String toAddress = "";

    @Column(nullable = false, length = 998)
    private String subject = "";

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "body_text", nullable = false, columnDefinition = "text")
    private String bodyText = "";

    @Column(name = "attachment_names", nullable = false, columnDefinition = "text")
    private String attachmentNames = "";

    /** JOB_CONVERSATION, JOB_ALERT or BOUNCE. */
    @Column(nullable = false, length = 20)
    private String category;

    /** REPLY, RECRUITER, COMPANY, or null when nothing matched. */
    @Column(name = "link_method", length = 20)
    private String linkMethod;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private Recruiter recruiter;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "email_log_id")
    private EmailLog emailLog;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    protected InboxMessage() {
    }

    public InboxMessage(String folder, long imapUid) {
        this.folder = folder;
        this.imapUid = imapUid;
    }

    public void link(String method, Recruiter recruiter, Company company, EmailLog emailLog) {
        this.linkMethod = method;
        this.recruiter = recruiter;
        this.company = company;
        this.emailLog = emailLog;
    }

    public Long getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    public long getImapUid() {
        return imapUid;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getBodyText() {
        return bodyText;
    }

    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }

    public String getAttachmentNames() {
        return attachmentNames;
    }

    public void setAttachmentNames(String attachmentNames) {
        this.attachmentNames = attachmentNames;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLinkMethod() {
        return linkMethod;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public Company getCompany() {
        return company;
    }

    public EmailLog getEmailLog() {
        return emailLog;
    }
}
