package com.jobmonitor.web;

import com.jobmonitor.sending.EmailLogRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.jobmonitor.sending.Recruiter;
import com.jobmonitor.sending.RecruiterRepository;
import com.jobmonitor.sending.RecruiterService;
import com.jobmonitor.sending.SendStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recruiters")
public class RecruitersController {

    /** The old recruiter list, copied into the project (544 unique addresses). */
    static final Path OLD_LIST = Path.of("data", "import", "old-recruiters.txt");

    public record Row(Long id, String email, String name, String company, String source,
                      boolean emailed, boolean doNotContact) {
    }

    private final RecruiterRepository recruiters;
    private final EmailLogRepository logs;
    private final RecruiterService recruiterService;

    public RecruitersController(RecruiterRepository recruiters, EmailLogRepository logs,
                                RecruiterService recruiterService) {
        this.recruiters = recruiters;
        this.logs = logs;
        this.recruiterService = recruiterService;
    }

    @GetMapping
    public String list(Model model) {
        Set<Long> emailed = logs.findRecruiterIdsWithStatus(SendStatus.SENT);
        List<Row> rows = recruiters.findAllByOrderByEmailAsc().stream().map(r -> toRow(r, emailed)).toList();
        model.addAttribute("rows", rows);
        model.addAttribute("oldListExists", Files.exists(OLD_LIST));
        return "recruiters";
    }

    @PostMapping("/import")
    public String importPasted(@RequestParam(defaultValue = "") String entries, RedirectAttributes redirect) {
        RecruiterService.Parsed parsed = RecruiterService.parse(entries);
        RecruiterService.ImportResult result = recruiterService.importEntries(parsed.valid(), "PASTED");
        redirect.addFlashAttribute(parsed.valid().isEmpty() ? "error" : "notice", summary(result, parsed.invalid()));
        return "redirect:/recruiters";
    }

    @PostMapping("/import-old")
    public String importOld(RedirectAttributes redirect) throws IOException {
        if (!Files.exists(OLD_LIST)) {
            redirect.addFlashAttribute("error", "The old list isn't in the project folder: " + OLD_LIST);
            return "redirect:/recruiters";
        }
        String text = Files.readString(OLD_LIST, StandardCharsets.UTF_8);
        RecruiterService.Parsed parsed = RecruiterService.parse(text);
        RecruiterService.ImportResult result = recruiterService.importEntries(parsed.valid(), "IMPORTED");
        redirect.addFlashAttribute("notice", summary(result, parsed.invalid()));
        return "redirect:/recruiters";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable long id, Model model) {
        Recruiter r = recruiters.findById(id)
                .orElseThrow(() -> new com.jobmonitor.NotFoundException("That recruiter doesn't exist."));
        model.addAttribute("recruiter", r);
        model.addAttribute("companyDomain", r.getCompany().getDomain() == null ? "" : r.getCompany().getDomain());
        model.addAttribute("sentCount", logs.countByRecruiterAndStatusAndSentAtGreaterThanEqual(
                r, SendStatus.SENT, java.time.Instant.EPOCH));
        return "recruiter-edit";
    }

    @PostMapping("/{id}")
    public String save(@PathVariable long id, @RequestParam(defaultValue = "") String name,
                       @RequestParam(defaultValue = "") String companyDomain, RedirectAttributes redirect) {
        try {
            recruiterService.update(id, name, companyDomain);
            redirect.addFlashAttribute("notice", "Recruiter saved.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recruiters/" + id;
    }

    @PostMapping("/{id}/do-not-contact")
    public String doNotContact(@PathVariable long id, @RequestParam boolean value, RedirectAttributes redirect) {
        recruiterService.setDoNotContact(id, value);
        redirect.addFlashAttribute("notice", value ? "Marked do not contact." : "No longer marked do not contact.");
        return "redirect:/recruiters";
    }

    private static Row toRow(Recruiter r, Set<Long> emailed) {
        return new Row(r.getId(), r.getEmail(), r.getName(),
                r.getCompany().isOthers() ? "Others" : r.getCompany().getName(),
                r.getSource(), emailed.contains(r.getId()), r.isDoNotContact());
    }

    private static String summary(RecruiterService.ImportResult result, List<String> invalid) {
        StringBuilder text = new StringBuilder("Added " + result.added() + ". Already in the list: "
                + result.alreadyKnown() + ".");
        if (result.repeated() > 0) {
            text.append(" Repeated in your input: ").append(result.repeated()).append(".");
        }
        if (!invalid.isEmpty()) {
            text.append(" Not email addresses (skipped): ").append(String.join(", ", invalid)).append(".");
        }
        return text.toString();
    }
}
