package com.jobmonitor.web;

import com.jobmonitor.NotFoundException;
import com.jobmonitor.masterdata.ResumeService;
import com.jobmonitor.masterdata.ResumeSummary;
import com.jobmonitor.masterdata.TemplateService;
import com.jobmonitor.sending.OutreachService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/compose")
public class ComposeController {

    private final TemplateService templates;
    private final ResumeService resumes;
    private final OutreachService outreach;

    public ComposeController(TemplateService templates, ResumeService resumes, OutreachService outreach) {
        this.templates = templates;
        this.resumes = resumes;
        this.outreach = outreach;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("templates", templates.list());
        model.addAttribute("resumeName", resumes.list().stream()
                .filter(ResumeSummary::active).map(ResumeSummary::fileName).findFirst().orElse(null));
        return "compose";
    }

    @PostMapping
    public String queue(@RequestParam long templateId, @RequestParam(defaultValue = "") String role,
                        @RequestParam(defaultValue = "") String recipients, RedirectAttributes redirect) {
        try {
            var result = outreach.queue(recipients, templates.get(templateId), role);
            List<String> parts = new ArrayList<>();
            parts.add("Queued " + result.queued() + (result.queued() == 1 ? " email" : " emails") + ".");
            if (!result.skipped().isEmpty()) {
                parts.add("Skipped " + result.skipped().size() + ": " + String.join("; ", result.skipped()) + ".");
            }
            if (!result.invalid().isEmpty()) {
                parts.add("Not an email address: " + String.join(", ", result.invalid()) + ".");
            }
            if (!result.warnings().isEmpty()) {
                parts.add(String.join(" ", result.warnings()));
            }
            redirect.addFlashAttribute(result.queued() > 0 ? "notice" : "error", String.join(" ", parts));
        } catch (IllegalStateException | NotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sent";
    }
}
