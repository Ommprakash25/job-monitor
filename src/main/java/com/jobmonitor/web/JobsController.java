package com.jobmonitor.web;

import com.jobmonitor.jobs.JobFeedService;
import com.jobmonitor.jobs.JobListing;
import com.jobmonitor.jobs.JobListingRepository;
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
@RequestMapping("/jobs")
public class JobsController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, HH:mm");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public record Row(String title, String company, String location, String posted, String url) {
    }

    private final JobListingRepository jobs;
    private final JobFeedService feed;

    public JobsController(JobListingRepository jobs, JobFeedService feed) {
        this.jobs = jobs;
        this.feed = feed;
    }

    @GetMapping
    public String list(Model model) {
        List<Row> rows = jobs.findTop200ByOrderByPostedAtDesc().stream().map(JobsController::toRow).toList();
        model.addAttribute("rows", rows);
        model.addAttribute("configured", feed.isConfigured());
        return "jobs";
    }

    @PostMapping("/refresh")
    public String refresh(RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("notice", "Found " + feed.refresh() + " new postings.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/jobs";
    }

    private static Row toRow(JobListing job) {
        return new Row(job.getTitle(), job.getCompany(), job.getLocation(),
                job.getPostedAt() == null ? "" : job.getPostedAt().atZone(ZONE).format(WHEN), job.getUrl());
    }
}
