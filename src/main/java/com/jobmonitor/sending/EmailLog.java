package com.jobmonitor.sending;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One row per email: what was sent (or queued, skipped or failed), exactly as written. */
@Entity
@Table(name = "email_log")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mail_account_address", nullable = false, length = 254)
    private String mailAccountAddress;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "to_address", nullable = false, length = 254)
    private String toAddress;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "text_body", nullable = false, columnDefinition = "text")
    private String textBody;

    @Column(name = "html_body", nullable = false, columnDefinition = "text")
    private String htmlBody;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "template_name", length = 120)
    private String templateName;

    @Column(nullable = false, length = 20)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SendStatus status;

    @Column(columnDefinition = "text")
    private String error;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt = Instant.now();

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "message_id", length = 998)
    private String messageId;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "bounced_at")
    private Instant bouncedAt;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Instant getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(Instant repliedAt) {
        this.repliedAt = repliedAt;
    }

    public Instant getBouncedAt() {
        return bouncedAt;
    }

    public void setBouncedAt(Instant bouncedAt) {
        this.bouncedAt = bouncedAt;
    }

    @Column(name = "in_reply_to", length = 998)
    private String inReplyTo;

    @Column(name = "thread_references", length = 4000)
    private String threadReferences;

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public String getThreadReferences() {
        return threadReferences;
    }

    public void setThreadReferences(String threadReferences) {
        this.threadReferences = threadReferences;
    }

    public Long getId() {
        return id;
    }

    public String getMailAccountAddress() {
        return mailAccountAddress;
    }

    public void setMailAccountAddress(String mailAccountAddress) {
        this.mailAccountAddress = mailAccountAddress;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
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

    public String getTextBody() {
        return textBody;
    }

    public void setTextBody(String textBody) {
        this.textBody = textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public void setHtmlBody(String htmlBody) {
        this.htmlBody = htmlBody;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public SendStatus getStatus() {
        return status;
    }

    public void setStatus(SendStatus status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}
