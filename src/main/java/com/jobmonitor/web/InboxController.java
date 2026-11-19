package com.jobmonitor.web;

import com.jobmonitor.inbox.InboxMessage;
import com.jobmonitor.inbox.InboxMessageRepository;
import com.jobmonitor.inbox.InboxSyncService;
import com.jobmonitor.sending.EmailLog;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inbox")
public class InboxController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public record Row(Long id, String when, String folder, String from, String subject, String category,
                      String linked, String method, String preview) {
    }

    public record FollowUp(Long id, String to, String company, String subject, String sentAt) {
    }

    private final InboxMessageRepository messages;
    private final InboxSyncService sync;

    public InboxController(InboxMessageRepository messages, InboxSyncService sync) {
        this.messages = messages;
        this.sync = sync;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "all") String filter, Model model) {
        List<Row> rows = messages.findTop500ByOrderByReceivedAtDesc().stream()
                .filter(m -> matches(m, filter))
                .map(InboxController::toRow)
                .toList();
        model.addAttribute("filter", filter);
        model.addAttribute("rows", rows);
        model.addAttribute("connected", sync.isConfigured());
        model.addAttribute("followUps", sync.needsFollowUp().stream().map(InboxController::toFollowUp).toList());
        return "inbox";
    }

    @PostMapping("/sync")
    public String syncNow(RedirectAttributes redirect) {
        try {
            InboxSyncService.SyncResult result = sync.sync();
            redirect.addFlashAttribute("notice", "Synced: " + result.added() + " new "
                    + (result.added() == 1 ? "message" : "messages") + ", " + result.linked() + " linked, "
                    + result.bounces() + " " + (result.bounces() == 1 ? "bounce" : "bounces") + ".");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inbox";
    }

    private static boolean matches(InboxMessage m, String filter) {
        return switch (filter) {
            case "replies" -> "REPLY".equals(m.getLinkMethod());
            case "recruiters" -> m.getLinkMethod() != null && !"REPLY".equals(m.getLinkMethod())
                    && "JOB_CONVERSATION".equals(m.getCategory());
            case "alerts" -> "JOB_ALERT".equals(m.getCategory());
            case "bounces" -> "BOUNCE".equals(m.getCategory());
            case "unlinked" -> m.getLinkMethod() == null && !"SENT".equals(m.getFolder());
            case "spam" -> "SPAM".equals(m.getFolder());
            case "sent" -> "SENT".equals(m.getFolder());
            default -> true;
        };
    }

    private static Row toRow(InboxMessage m) {
        String linked = m.getRecruiter() != null ? m.getRecruiter().getEmail()
                : m.getCompany() != null ? m.getCompany().getName() : "Not linked";
        String preview = m.getBodyText().strip().replaceAll("\\s+", " ");
        if (preview.length() > 160) {
            preview = preview.substring(0, 160) + "…";
        }
        return new Row(m.getId(), m.getReceivedAt().atZone(ZONE).format(WHEN), m.getFolder(),
                m.getFromName().isBlank() ? m.getFromAddress() : m.getFromName() + " <" + m.getFromAddress() + ">",
                m.getSubject(), m.getCategory(), linked, m.getLinkMethod() == null ? "" : m.getLinkMethod(), preview);
    }

    private static FollowUp toFollowUp(EmailLog l) {
        return new FollowUp(l.getId(), l.getToAddress(),
                l.getCompany().isOthers() ? "Others" : l.getCompany().getName(), l.getSubject(),
                l.getSentAt().atZone(ZONE).format(WHEN));
    }
}
