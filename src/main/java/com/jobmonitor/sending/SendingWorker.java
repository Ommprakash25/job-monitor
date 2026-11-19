package com.jobmonitor.sending;

import com.jobmonitor.mail.MailGateway;
import com.jobmonitor.mail.MailGatewayException;
import com.jobmonitor.mail.OutgoingEmail;
import com.jobmonitor.masterdata.DayOffRepository;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountRepository;
import com.jobmonitor.masterdata.ProfileService;
import com.jobmonitor.masterdata.Resume;
import com.jobmonitor.masterdata.ResumeRepository;
import com.jobmonitor.masterdata.SendingSettings;
import com.jobmonitor.masterdata.SendingSettingsService;
import com.jobmonitor.security.SecretCipher;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends queued emails one at a time, inside the sending rules in Master data.
 * Checked every 30 seconds. Sends nothing while paused.
 */
@Component
public class SendingWorker {

    private static final Logger log = LoggerFactory.getLogger(SendingWorker.class);
    /** The first email of the day goes out at a random time in the first half hour. */
    private static final long RANDOM_START_SECONDS = 30 * 60;
    private static final int MAX_ATTEMPTS = 3;
    /** Don't judge the bounce rate from the first few emails of a day. */
    private static final int BOUNCE_CHECK_MIN_SENDS = 10;
    private static final Duration RETRY_AFTER = Duration.ofMinutes(15);
    private static final Duration DEFER_COMPANY_LIMIT = Duration.ofHours(1);

    private final SendingSettingsService settingsService;
    private final DayOffRepository daysOff;
    private final EmailLogRepository logs;
    private final ResumeRepository resumes;
    private final MailAccountRepository accounts;
    private final ProfileService profiles;
    private final SecretCipher cipher;
    private final MailGateway gateway;

    public SendingWorker(SendingSettingsService settingsService, DayOffRepository daysOff, EmailLogRepository logs,
                         ResumeRepository resumes, MailAccountRepository accounts, ProfileService profiles,
                         SecretCipher cipher, MailGateway gateway) {
        this.settingsService = settingsService;
        this.daysOff = daysOff;
        this.logs = logs;
        this.resumes = resumes;
        this.accounts = accounts;
        this.profiles = profiles;
        this.cipher = cipher;
        this.gateway = gateway;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    @Transactional
    public void tick() {
        runOnce(Instant.now());
    }

    /** At most one email per call. */
    void runOnce(Instant now) {
        SendingSettings s = settingsService.get();
        if (s.isSendingPaused()) {
            return;
        }
        ZoneId zone = ZoneId.of(s.getTimeZone());
        ZonedDateTime local = now.atZone(zone);
        LocalDate today = local.toLocalDate();
        if (!s.getSendingDaySet().contains(local.getDayOfWeek()) || daysOff.existsByDay(today)) {
            return;
        }
        LocalTime time = local.toLocalTime();
        if (time.isBefore(s.getWindowStart()) || !time.isBefore(s.getWindowEnd())) {
            return;
        }

        Instant dayStart = today.atStartOfDay(zone).toInstant();
        long sentToday = logs.countByStatusAndSentAtGreaterThanEqual(SendStatus.SENT, dayStart);
        if (sentToday >= s.getDailyLimit()) {
            return;
        }
        if (s.getBouncePausePercent() != null && sentToday >= BOUNCE_CHECK_MIN_SENDS) {
            long bouncedToday = logs.countByBouncedAtGreaterThanEqual(dayStart);
            if (bouncedToday * 100 >= sentToday * s.getBouncePausePercent()) {
                String reason = "Paused automatically: " + bouncedToday + " of today's " + sentToday
                        + " emails bounced (limit " + s.getBouncePausePercent() + "%). Check the recruiter list, "
                        + "then press Start sending.";
                s.setSendingPaused(true);
                s.setPauseReason(reason);
                log.warn(reason);
                return;
            }
        }

        Instant windowStart = today.atTime(s.getWindowStart()).atZone(zone).toInstant();
        Instant windowEnd = today.atTime(s.getWindowEnd()).atZone(zone).toInstant();
        if (sentToday == 0 && (s.getNextSendAt() == null || s.getNextSendAt().isBefore(windowStart))) {
            long windowSeconds = Duration.between(windowStart, windowEnd).getSeconds();
            long offset = ThreadLocalRandom.current().nextLong(Math.min(RANDOM_START_SECONDS, windowSeconds));
            s.setNextSendAt(windowStart.plusSeconds(offset));
        }
        if (s.getNextSendAt() != null && now.isBefore(s.getNextSendAt())) {
            return;
        }

        Optional<EmailLog> next = logs.findFirstByStatusAndDueAtLessThanEqualOrderByDueAtAsc(SendStatus.QUEUED, now);
        if (next.isEmpty()) {
            return;
        }
        EmailLog mail = next.get();
        Recruiter recruiter = mail.getRecruiter();
        Company company = mail.getCompany();

        if (recruiter.isDoNotContact()) {
            skip(mail, "Marked do not contact.");
            return;
        }
        if (s.getRecontactAfterDays() != null) {
            Instant since = now.minus(Duration.ofDays(s.getRecontactAfterDays()));
            if (logs.countByRecruiterAndStatusAndSentAtGreaterThanEqual(recruiter, SendStatus.SENT, since) > 0) {
                skip(mail, "Already emailed within the re-contact period.");
                return;
            }
        }
        if (!company.isOthers()) {
            Instant since = now.minus(Duration.ofDays(s.getCompanyLimitDays()));
            if (logs.countByCompanyAndStatusAndSentAtGreaterThanEqual(company, SendStatus.SENT, since)
                    >= s.getCompanyLimitCount()) {
                mail.setDueAt(now.plus(DEFER_COMPANY_LIMIT));
                return;
            }
        }

        Optional<Resume> resume = resumes.findFirstByActiveTrue();
        Optional<MailAccount> account = accounts.findFirstByActiveTrue();
        if (resume.isEmpty() || account.isEmpty() || !account.get().hasSecret()) {
            return;
        }
        send(s, mail, now, resume.get(), account.get());
    }

    private void send(SendingSettings s, EmailLog mail, Instant now, Resume resume, MailAccount account) {
        try {
            String messageId = gateway.send(account.getEmailAddress(), profiles.get().getFullName(),
                    cipher.decrypt(account.getSecretEncrypted()),
                    new OutgoingEmail(mail.getToAddress(), mail.getSubject(), mail.getTextBody(), mail.getHtmlBody(),
                            resume.getFileName(), resume.getContentType(), resume.getContent(),
                            mail.getInReplyTo(), mail.getThreadReferences()));
            mail.setStatus(SendStatus.SENT);
            mail.setMessageId(messageId);
            mail.setSentAt(now);
            mail.setError(null);
            mail.setAttempts(mail.getAttempts() + 1);
            long gap = ThreadLocalRandom.current().nextLong(
                    s.getGapMinMinutes() * 60L, s.getGapMaxMinutes() * 60L + 1);
            s.setNextSendAt(now.plusSeconds(gap));
            log.info("Sent to {}", mail.getToAddress());
        } catch (MailGatewayException | IllegalStateException e) {
            mail.setAttempts(mail.getAttempts() + 1);
            mail.setError(e.getMessage());
            if (mail.getAttempts() >= MAX_ATTEMPTS) {
                mail.setStatus(SendStatus.FAILED);
            } else {
                mail.setDueAt(now.plus(RETRY_AFTER));
            }
            log.warn("Couldn't send to {}: {}", mail.getToAddress(), e.getMessage());
        }
    }

    private static void skip(EmailLog mail, String reason) {
        mail.setStatus(SendStatus.SKIPPED);
        mail.setError(reason);
    }
}
