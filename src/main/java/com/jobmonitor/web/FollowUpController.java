package com.jobmonitor.web;

import com.jobmonitor.NotFoundException;
import com.jobmonitor.sending.EmailLog;
import com.jobmonitor.sending.EmailLogRepository;
import com.jobmonitor.sending.OutreachService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Writes a follow-up that continues the conversation of an email already sent. */
@Controller
@RequestMapping("/follow-ups")
public class FollowUpController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final EmailLogRepository logs;
    private final OutreachService outreach;

    public FollowUpController(EmailLogRepository logs, OutreachService outreach) {
        this.logs = logs;
        this.outreach = outreach;
    }

    @GetMapping("/{id}")
    public String form(@PathVariable long id, Model model) {
        EmailLog original = logs.findById(id).orElseThrow(() -> new NotFoundException("That email doesn't exist."));
        model.addAttribute("original", original);
        model.addAttribute("sentAt", original.getSentAt() == null ? "" : original.getSentAt()
                .atZone(ZoneId.of("Asia/Kolkata")).format(WHEN));
        model.addAttribute("body", "Hi {name},\n\nI'm following up on my email below in case it got lost.\n\nRegards,\n");
        return "follow-up";
    }

    @PostMapping("/{id}")
    public String queue(@PathVariable long id, @RequestParam(defaultValue = "") String body,
                        RedirectAttributes redirect) {
        try {
            outreach.queueFollowUp(id, body);
            redirect.addFlashAttribute("notice", "Follow-up queued in the same conversation. It goes out with your sending rules.");
            return "redirect:/sent";
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/follow-ups/" + id;
        }
    }
}
