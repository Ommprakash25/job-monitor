package com.jobmonitor.web;

import com.jobmonitor.masterdata.Resume;
import com.jobmonitor.masterdata.ResumeService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/resumes")
public class ResumeController {

    private final ResumeService resumes;

    public ResumeController(ResumeService resumes) {
        this.resumes = resumes;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("resumes", resumes.list());
        return "master/resumes";
    }

    @PostMapping
    public String upload(@RequestParam(value = "file", required = false) MultipartFile file,
                         RedirectAttributes redirect) throws IOException {
        try {
            Resume saved = resumes.upload(file);
            redirect.addFlashAttribute("notice", saved.isActive()
                    ? "Uploaded " + saved.getFileName() + ". It's your active resume."
                    : "Uploaded " + saved.getFileName() + ". Make it active to attach it to emails.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master/resumes";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable long id, RedirectAttributes redirect) {
        resumes.activate(id);
        redirect.addFlashAttribute("notice", "Active resume changed.");
        return "redirect:/master/resumes";
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable long id) {
        Resume resume = resumes.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resume.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(resume.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(resume.getContent());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        try {
            resumes.delete(id);
            redirect.addFlashAttribute("notice", "Resume deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master/resumes";
    }
}
