package com.jobmonitor.web;

import com.jobmonitor.masterdata.EmailTemplate;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountService;
import com.jobmonitor.masterdata.ProfileService;
import com.jobmonitor.masterdata.ResumeService;
import com.jobmonitor.masterdata.SendingSettings;
import com.jobmonitor.masterdata.SendingSettingsForm;
import com.jobmonitor.masterdata.SendingSettingsService;
import com.jobmonitor.masterdata.TemplateService;
import com.jobmonitor.sending.EmailLogRepository;
import com.jobmonitor.sending.SendStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** One item on the setup checklist. {@code done} is null for items that are just a suggestion. */
    public record SetupStep(String title, String hint, Boolean done, String link, String linkLabel) {
    }

    private final ProfileService profiles;
    private final ResumeService resumes;
    private final TemplateService templates;
    private final MailAccountService accounts;
    private final SendingSettingsService settings;
    private final EmailLogRepository logs;

    public DashboardController(ProfileService profiles, ResumeService resumes, TemplateService templates,
                               MailAccountService accounts, SendingSettingsService settings,
                               EmailLogRepository logs) {
        this.profiles = profiles;
        this.resumes = resumes;
        this.templates = templates;
        this.accounts = accounts;
        this.settings = settings;
        this.logs = logs;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        Optional<EmailTemplate> template = templates.defaultTemplate();
        Optional<MailAccount> account = accounts.current();
        SendingSettings sending = settings.get();
        ZoneId zone = ZoneId.of(sending.getTimeZone());
        Instant now = Instant.now();
        Instant dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();

        List<SetupStep> steps = List.of(
                new SetupStep("Add your name",
                        "Recruiters see it as the sender, for example \"Priya Sharma <you@gmail.com>\".",
                        !profiles.get().getFullName().isBlank(), "/master/profile", "Open profile"),
                new SetupStep("Upload your resume",
                        "The active resume is attached to every email.",
                        resumes.hasActive(), "/master/resumes", "Open resumes"),
                new SetupStep("Fill in your details in the template",
                        "Replace the [square brackets] in the starter template with your own details.",
                        template.map(t -> !templates.hasUnfilledDetails(t)).orElse(false),
                        template.map(t -> "/master/templates/" + t.getId()).orElse("/master/templates"),
                        "Open template"),
                new SetupStep("Add the Gmail account",
                        "Sign in with the Gmail address and app password.",
                        account.map(a -> Boolean.TRUE.equals(a.getLastCheckOk())).orElse(false),
                        "/master/account", "Open Gmail account"));
        boolean setupDone = steps.stream().allMatch(s -> Boolean.TRUE.equals(s.done()));

        long sentToday = logs.countByStatusAndSentAtGreaterThanEqual(SendStatus.SENT, dayStart);
        model.addAttribute("setupDone", setupDone);
        model.addAttribute("steps", steps);
        model.addAttribute("doneCount", steps.stream().filter(s -> Boolean.TRUE.equals(s.done())).count());
        model.addAttribute("stepCount", steps.size());
        model.addAttribute("paused", sending.isSendingPaused());
        model.addAttribute("pauseReason", sending.getPauseReason());
        model.addAttribute("sentToday", sentToday);
        model.addAttribute("dailyLimit", sending.getDailyLimit());
        model.addAttribute("waiting", logs.countByStatus(SendStatus.QUEUED));
        model.addAttribute("nextSend", sending.getNextSendAt() != null && sending.getNextSendAt().isAfter(now)
                ? sending.getNextSendAt().atZone(zone).format(TIME) : null);
        model.addAttribute("bouncesToday", logs.countByBouncedAtGreaterThanEqual(dayStart));
        model.addAttribute("bouncesTotal", logs.countByBouncedAtIsNotNull());
        long totalSent = logs.countByStatus(SendStatus.SENT);
        long replies = logs.countByRepliedAtIsNotNull();
        model.addAttribute("repliesTotal", replies);
        model.addAttribute("replyRate", totalSent == 0 ? "—" : Math.round(replies * 100.0 / totalSent) + "%");
        model.addAttribute("followUps", logs.countByStatusAndSentAtBeforeAndRepliedAtIsNullAndBouncedAtIsNull(
                SendStatus.SENT, now.minusSeconds(sending.getFollowUpAfterDays() * 86_400L)));
        model.addAttribute("capacity", SendingSettingsForm.from(sending).capacity());
        model.addAttribute("settings", sending);
        return "dashboard";
    }

    @GetMapping("/master")
    public String masterData() {
        return "redirect:/master/profile";
    }
}
