package com.jobmonitor.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobmonitor.NotFoundException;
import com.jobmonitor.masterdata.EmailTemplate;
import com.jobmonitor.masterdata.ResumeService;
import com.jobmonitor.masterdata.ResumeSummary;
import com.jobmonitor.masterdata.TemplateService;
import com.jobmonitor.sending.OutreachService;
import com.jobmonitor.sending.PreviewRow;
import com.jobmonitor.sending.UploadParser;
import com.jobmonitor.sending.UploadRow;
import com.jobmonitor.sending.UploadService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Upload an Excel or CSV list, check the preview, then queue the ready rows. */
@Controller
@RequestMapping("/compose/upload")
public class UploadController {

    private final TemplateService templates;
    private final ResumeService resumes;
    private final UploadService uploads;
    private final ObjectMapper json;

    public UploadController(TemplateService templates, ResumeService resumes, UploadService uploads,
                            ObjectMapper json) {
        this.templates = templates;
        this.resumes = resumes;
        this.uploads = uploads;
        this.json = json;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("templates", templates.list());
        model.addAttribute("resumeName", resumes.list().stream()
                .filter(ResumeSummary::active).map(ResumeSummary::fileName).findFirst().orElse(null));
        return "upload";
    }

    @PostMapping("/preview")
    public String preview(@RequestParam("file") MultipartFile file, @RequestParam long templateId,
                          @RequestParam(defaultValue = "") String role, Model model, RedirectAttributes redirect)
            throws IOException {
        try (InputStream in = file.getInputStream()) {
            List<UploadRow> rows = UploadParser.parse(file.getOriginalFilename(), in);
            if (rows.isEmpty()) {
                redirect.addFlashAttribute("error", "The file has no recruiters in it.");
                return "redirect:/compose/upload";
            }
            List<PreviewRow> previews = uploads.preview(rows, role);
            long ready = previews.stream().filter(PreviewRow::ready).count();
            model.addAttribute("previews", previews);
            model.addAttribute("ready", ready);
            model.addAttribute("notReady", previews.size() - ready);
            model.addAttribute("rowsJson", json.writeValueAsString(rows));
            model.addAttribute("templateId", templateId);
            model.addAttribute("role", role);
            model.addAttribute("templateName", templates.get(templateId).getName());
            model.addAttribute("fileName", file.getOriginalFilename());
            return "upload-preview";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/compose/upload";
        }
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String rowsJson, @RequestParam long templateId,
                          @RequestParam(defaultValue = "") String role, RedirectAttributes redirect)
            throws IOException {
        try {
            List<UploadRow> rows = json.readValue(rowsJson, new TypeReference<List<UploadRow>>() {
            });
            EmailTemplate template = templates.get(templateId);
            OutreachService.QueueResult result = uploads.confirm(rows, template, role);
            String message = "Queued " + result.queued() + (result.queued() == 1 ? " email" : " emails") + ".";
            if (!result.skipped().isEmpty()) {
                message += " Left out " + result.skipped().size() + ": " + String.join("; ", result.skipped()) + ".";
            }
            redirect.addFlashAttribute(result.queued() > 0 ? "notice" : "error", message);
            return "redirect:/sent";
        } catch (IllegalStateException | NotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/compose/upload";
        }
    }
}
