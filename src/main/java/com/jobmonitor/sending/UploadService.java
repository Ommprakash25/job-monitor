package com.jobmonitor.sending;

import com.jobmonitor.masterdata.EmailTemplate;
import com.jobmonitor.masterdata.FreeMailDomainRepository;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountRepository;
import com.jobmonitor.masterdata.Resume;
import com.jobmonitor.masterdata.ResumeRepository;
import com.jobmonitor.masterdata.SendingSettings;
import com.jobmonitor.masterdata.SendingSettingsService;
import com.jobmonitor.masterdata.TemplateService;
import com.jobmonitor.template.PlaceholderValues;
import com.jobmonitor.template.RenderedEmail;
import com.jobmonitor.template.TemplateRenderer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Previews an uploaded list, then queues the rows that are ready. Nothing is sent here. */
@Service
@Transactional
public class UploadService {

    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9][a-z0-9._+-]*@(?:[a-z0-9-]+\\.)+[a-z]{2,63}$");

    private final RecruiterRepository recruiters;
    private final RecruiterService recruiterService;
    private final EmailLogRepository logs;
    private final MailAccountRepository accounts;
    private final ResumeRepository resumes;
    private final TemplateService templates;
    private final SendingSettingsService settings;
    private final FreeMailDomainRepository freeMailDomains;
    private final TemplateRenderer renderer = new TemplateRenderer();

    public UploadService(RecruiterRepository recruiters, RecruiterService recruiterService, EmailLogRepository logs,
                         MailAccountRepository accounts, ResumeRepository resumes, TemplateService templates,
                         SendingSettingsService settings, FreeMailDomainRepository freeMailDomains) {
        this.recruiters = recruiters;
        this.recruiterService = recruiterService;
        this.logs = logs;
        this.accounts = accounts;
        this.resumes = resumes;
        this.templates = templates;
        this.settings = settings;
        this.freeMailDomains = freeMailDomains;
    }

    /** Decides what happens to each row, without changing anything. */
    public List<PreviewRow> preview(List<UploadRow> rows, String batchRole) {
        SendingSettings s = settings.get();
        Instant now = Instant.now();
        Instant recontactSince = s.getRecontactAfterDays() == null
                ? null : now.minus(Duration.ofDays(s.getRecontactAfterDays()));
        Set<String> seen = new HashSet<>();
        List<PreviewRow> out = new ArrayList<>();
        for (UploadRow row : rows) {
            String email = row.email() == null ? "" : row.email().strip().toLowerCase(Locale.ROOT);
            String name = row.name() == null ? "" : row.name().strip();
            String role = row.role() == null || row.role().isBlank() ? batchRole : row.role().strip();
            String status;
            boolean ready = false;
            if (!EMAIL.matcher(email).matches()) {
                status = "Not an email address";
            } else if (!seen.add(email)) {
                status = "Repeated in the file";
            } else {
                Optional<Recruiter> known = recruiters.findByEmail(email);
                if (known.isPresent() && known.get().isDoNotContact()) {
                    status = "Marked do not contact";
                } else if (known.isPresent() && logs.existsByRecruiterAndStatus(known.get(), SendStatus.QUEUED)) {
                    status = "Already waiting to send";
                } else if (known.isPresent() && recontactSince != null && logs.countByRecruiterAndStatusAndSentAtGreaterThanEqual(
                        known.get(), SendStatus.SENT, recontactSince) > 0) {
                    status = "Emailed recently (re-contact rule)";
                } else {
                    status = "Ready";
                    ready = true;
                }
            }
            out.add(new PreviewRow(row.line(), email, name, companyLabel(email), role, status, ready));
        }
        return out;
    }

    /** Queues the rows that are ready, using the template. */
    public OutreachService.QueueResult confirm(List<UploadRow> rows, EmailTemplate template, String batchRole) {
        MailAccount account = accounts.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Add the Gmail account first, in Master data."));
        Resume resume = resumes.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Upload a resume and make it active first."));
        if (templates.hasUnfilledDetails(template)) {
            throw new IllegalStateException("Fill in the [square brackets] in \"" + template.getName() + "\" before sending.");
        }
        String roleFallback = settings.get().getRoleFallback();
        Instant now = Instant.now();
        int queued = 0;
        List<String> skipped = new ArrayList<>();
        for (PreviewRow row : preview(rows, batchRole)) {
            if (!row.ready()) {
                skipped.add((row.email().isBlank() ? "row " + row.line() : row.email()) + " (" + row.status() + ")");
                continue;
            }
            Recruiter recruiter = recruiterService.findOrCreate(
                    new RecruiterService.Entry(row.email(), row.name() == null ? "" : row.name()), "UPLOAD");
            Company company = recruiter.getCompany();
            RenderedEmail rendered = renderer.render(template.getSubject(), template.getBody(),
                    new PlaceholderValues(recruiter.getName(), company.isOthers() ? null : company.getName(),
                            row.role(), roleFallback));
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
            mail.setChannel("UPLOAD");
            mail.setStatus(SendStatus.QUEUED);
            mail.setAttempts(0);
            mail.setQueuedAt(now);
            mail.setDueAt(now);
            logs.save(mail);
            queued++;
        }
        return new OutreachService.QueueResult(queued, skipped, List.of(), List.of());
    }

    private String companyLabel(String email) {
        int at = email.indexOf('@');
        if (at < 0) {
            return "";
        }
        String domain = email.substring(at + 1);
        if (freeMailDomains.existsById(domain)) {
            return "Others";
        }
        return RecruiterService.companyName(domain);
    }
}
