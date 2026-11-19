package com.jobmonitor.web;

import com.jobmonitor.masterdata.DomainListService;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The two simple lists in Master data: free-mail domains and job-alert senders. */
@Controller
public class DomainListController {

    private final DomainListService lists;

    public DomainListController(DomainListService lists) {
        this.lists = lists;
    }

    @GetMapping("/master/free-mail")
    public String freeMail(Model model) {
        return page(model, "free-mail", "Free-mail domains",
                "Recruiters writing from these domains get the company \"Others\", because the domain says nothing about where they work.",
                "gmail.com", lists.freeMailDomains());
    }

    @PostMapping("/master/free-mail")
    public String addFreeMail(@RequestParam("value") String value, RedirectAttributes redirect) {
        return add(value, lists::addFreeMailDomain, "/master/free-mail", redirect);
    }

    @PostMapping("/master/free-mail/delete")
    public String deleteFreeMail(@RequestParam("value") String value, RedirectAttributes redirect) {
        return delete(value, lists::removeFreeMailDomain, "/master/free-mail", redirect);
    }

    @GetMapping("/master/job-alerts")
    public String jobAlerts(Model model) {
        return page(model, "job-alerts", "Job-alert senders",
                "Email from these senders is filed as a job-portal alert instead of a conversation. Add a domain (naukri.com) or a full address.",
                "naukri.com", lists.jobAlertSenders());
    }

    @PostMapping("/master/job-alerts")
    public String addJobAlert(@RequestParam("value") String value, RedirectAttributes redirect) {
        return add(value, lists::addJobAlertSender, "/master/job-alerts", redirect);
    }

    @PostMapping("/master/job-alerts/delete")
    public String deleteJobAlert(@RequestParam("value") String value, RedirectAttributes redirect) {
        return delete(value, lists::removeJobAlertSender, "/master/job-alerts", redirect);
    }

    private String page(Model model, String key, String title, String description, String example,
                        List<String> items) {
        model.addAttribute("listKey", key);
        model.addAttribute("title", title);
        model.addAttribute("description", description);
        model.addAttribute("example", example);
        model.addAttribute("items", items);
        model.addAttribute("action", "/master/" + key);
        return "master/domain-list";
    }

    private String add(String value, Function<String, String> adder, String back, RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("notice", "Added " + adder.apply(value) + ".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("typedValue", value);
        }
        return "redirect:" + back;
    }

    private String delete(String value, Consumer<String> remover, String back, RedirectAttributes redirect) {
        remover.accept(value);
        redirect.addFlashAttribute("notice", "Removed " + value + ".");
        return "redirect:" + back;
    }
}
