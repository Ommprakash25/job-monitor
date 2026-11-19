package com.jobmonitor.sending;

import com.jobmonitor.masterdata.EmailTemplate;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountRepository;
import com.jobmonitor.masterdata.Resume;
import com.jobmonitor.masterdata.ResumeRepository;
import com.jobmonitor.masterdata.SendingSettingsService;
import com.jobmonitor.masterdata.TemplateService;
import com.jobmonitor.template.PlaceholderValues;
import com.jobmonitor.template.RenderedEmail;
import com.jobmonitor.template.TemplateRenderer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a list of recruiters and a template into queued emails. Nothing is sent here. */
@Service
@Transactional
public class OutreachService {

    public record QueueResult(int queued, List<String> skipped, List<String> invalid, List<String> warnings) {
    }

    private final RecruiterService recruiterService;
    private final EmailLogRepository logs;
    private final MailAccountRepository accounts;
    private final ResumeRepository resumes;
    private final TemplateService templates;
    private final SendingSettingsService settings;
    private final TemplateRenderer renderer = new TemplateRenderer();

    public OutreachService(RecruiterService recruiterService, EmailLogRepository logs,
                           MailAccountRepository accounts, ResumeRepository resumes,
                           TemplateService templates, SendingSettingsService settings) {
        this.recruiterService = recruiterService;
        this.logs = logs;
        this.accounts = accounts;
        this.resumes = resumes;
        this.templates = templates;
        this.settings = settings;
    }

    public QueueResult queue(String recipientsText, EmailTemplate template, String role) {
        MailAccount account = accounts.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Add the Gmail account first, in Master data."));
        Resume resume = resumes.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Upload a resume and make it active first."));
        if (templates.hasUnfilledDetails(template)) {
            throw new IllegalStateException("Fill in the [square brackets] in \"" + template.getName() + "\" before sending.");
        }

        RecruiterService.Parsed parsed = RecruiterService.parse(recipientsText);
        String roleFallback = settings.get().getRoleFallback();
        Instant now = Instant.now();
        List<String> skipped = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        int queued = 0;

        for (RecruiterService.Entry entry : parsed.valid()) {
            if (!seen.add(entry.email())) {
                skipped.add(entry.email() + " (repeated in this list)");
                continue;
            }
            Recruiter recruiter = recruiterService.findOrCreate(entry, "COMPOSE");
            if (recruiter.isDoNotContact()) {
                skipped.add(entry.email() + " (marked do not contact)");
                continue;
            }
            if (logs.existsByRecruiterAndStatus(recruiter, SendStatus.QUEUED)) {
                skipped.add(entry.email() + " (already queued)");
                continue;
            }

            Company company = recruiter.getCompany();
            RenderedEmail rendered = renderer.render(template.getSubject(), template.getBody(),
                    new PlaceholderValues(recruiter.getName(), company.isOthers() ? null : company.getName(),
                            role, roleFallback));
            warnings.addAll(rendered.warnings());

            EmailLog mail = new EmailLog();
            mail.setMailAccountAddress(account.getEmailAddress());
            mail.setRecruiter(recruiter);
            mail.setCompany(company);
            mail.setToAddress(recruiter.getEmail());
            mail.setSubject(rendered.subject());
            mail.setTextBody(rendered.textBody());
            mail.setHtmlBody(rendered.htmlBody());
            mail.setAttachmentName(resume.getFileName());
            mail.setTemplateName(template.getName());
            mail.setChannel("COMPOSE");
            mail.setStatus(SendStatus.QUEUED);
            mail.setAttempts(0);
            mail.setQueuedAt(now);
            mail.setDueAt(now);
            logs.save(mail);
            queued++;
        }
        return new QueueResult(queued, skipped, parsed.invalid(), List.copyOf(warnings));
    }

    /**
     * Queues a follow-up that continues the original email's conversation. Only for sent
     * emails with no reply and no bounce, to recruiters not marked do-not-contact.
     */
    public void queueFollowUp(long originalId, String bodyText) {
        EmailLog original = logs.findById(originalId)
                .orElseThrow(() -> new com.jobmonitor.NotFoundException("That email doesn't exist."));
        if (original.getStatus() != SendStatus.SENT) {
            throw new IllegalStateException("Only emails that were sent can be followed up.");
        }
        if (original.getRepliedAt() != null) {
            throw new IllegalStateException("This recruiter has already replied. Reply to them from Gmail instead.");
        }
        if (original.getBouncedAt() != null) {
            throw new IllegalStateException("That email bounced, so there's no one to follow up with.");
        }
        if (original.getMessageId() == null) {
            throw new IllegalStateException("Gmail's reference for that email is missing, so the follow-up can't join its conversation.");
        }
        Recruiter recruiter = original.getRecruiter();
        if (recruiter.isDoNotContact()) {
            throw new IllegalStateException("This recruiter is marked do not contact.");
        }
        if (logs.existsByRecruiterAndStatus(recruiter, SendStatus.QUEUED)) {
            throw new IllegalStateException("An email to this recruiter is already waiting to be sent.");
        }
        MailAccount account = accounts.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Add the Gmail account first, in Master data."));
        Resume resume = resumes.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Upload a resume and make it active first."));

        String subject = original.getSubject().startsWith("Re:") ? original.getSubject() : "Re: " + original.getSubject();
        RenderedEmail rendered = renderer.render(subject, bodyText,
                new PlaceholderValues(recruiter.getName(), null, null, settings.get().getRoleFallback()));
        if (rendered.warnings().stream().anyMatch(w -> w.startsWith("Still to fill in"))) {
            throw new IllegalStateException("Remove the [square brackets] from the follow-up before queueing it.");
        }

        Instant now = Instant.now();
        String references = original.getThreadReferences() == null || original.getThreadReferences().isBlank()
                ? original.getMessageId() : original.getThreadReferences() + " " + original.getMessageId();
        EmailLog mail = new EmailLog();
        mail.setMailAccountAddress(account.getEmailAddress());
        mail.setRecruiter(recruiter);
        mail.setCompany(recruiter.getCompany());
        mail.setToAddress(recruiter.getEmail());
        mail.setSubject(rendered.subject());
        mail.setTextBody(rendered.textBody());
        mail.setHtmlBody(rendered.htmlBody());
        mail.setAttachmentName(resume.getFileName());
        mail.setTemplateName("Follow-up");
        mail.setChannel("FOLLOW_UP");
        mail.setStatus(SendStatus.QUEUED);
        mail.setAttempts(0);
        mail.setQueuedAt(now);
        mail.setDueAt(now);
        mail.setInReplyTo(original.getMessageId());
        mail.setThreadReferences(references);
        logs.save(mail);
    }
}
