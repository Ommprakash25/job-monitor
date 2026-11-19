package com.jobmonitor.web;

import com.jobmonitor.masterdata.SendingSettings;
import com.jobmonitor.masterdata.SendingSettingsService;
import com.jobmonitor.sending.EmailLog;
import com.jobmonitor.sending.EmailLogRepository;
import com.jobmonitor.sending.SendStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sent")
public class SentController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    /** One row of the log, already formatted for the page. */
    public record Row(String when, String status, String to, String company, String subject,
                      String attachment, String note) {
    }

    private final SendingSettingsService settingsService;
    private final EmailLogRepository logs;

    public SentController(SendingSettingsService settingsService, EmailLogRepository logs) {
        this.settingsService = settingsService;
        this.logs = logs;
    }

    @GetMapping
    public String list(Model model) {
        SendingSettings s = settingsService.get();
        ZoneId zone = ZoneId.of(s.getTimeZone());
        Instant dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant now = Instant.now();

        List<Row> rows = logs.findTop200ByOrderByQueuedAtDesc().stream().map(l -> toRow(l, zone)).toList();
        model.addAttribute("rows", rows);
        model.addAttribute("paused", s.isSendingPaused());
        model.addAttribute("pauseReason", s.getPauseReason());
        model.addAttribute("sentToday", logs.countByStatusAndSentAtGreaterThanEqual(SendStatus.SENT, dayStart));
        model.addAttribute("dailyLimit", s.getDailyLimit());
        model.addAttribute("queued", logs.countByStatus(SendStatus.QUEUED));
        model.addAttribute("nextSend", s.getNextSendAt() != null && s.getNextSendAt().isAfter(now)
                ? s.getNextSendAt().atZone(zone).format(WHEN) : null);
        return "sent";
    }

    @PostMapping("/pause")
    public String pause(RedirectAttributes redirect) {
        settingsService.setPaused(true);
        redirect.addFlashAttribute("notice", "Sending paused. Nothing will be sent until you start it again.");
        return "redirect:/sent";
    }

    @PostMapping("/resume")
    public String resume(RedirectAttributes redirect) {
        settingsService.setPaused(false);
        redirect.addFlashAttribute("notice", "Sending started. Emails go out one at a time, within your sending rules.");
        return "redirect:/sent";
    }

    private static Row toRow(EmailLog l, ZoneId zone) {
        Instant when = l.getSentAt() != null ? l.getSentAt() : l.getQueuedAt();
        return new Row(when.atZone(zone).format(WHEN), l.getStatus().name(), l.getToAddress(),
                l.getCompany().isOthers() ? "Others" : l.getCompany().getName(), l.getSubject(),
                l.getAttachmentName(), l.getError());
    }
}
