package com.jobmonitor.web;

import com.jobmonitor.masterdata.DayOffForm;
import com.jobmonitor.masterdata.DayOffService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/days-off")
public class DayOffController {

    private final DayOffService daysOff;

    public DayOffController(DayOffService daysOff) {
        this.daysOff = daysOff;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("form", new DayOffForm());
        return page(model);
    }

    @PostMapping
    public String add(@Valid @ModelAttribute("form") DayOffForm form, BindingResult result, Model model,
                      RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                daysOff.add(form);
                redirect.addFlashAttribute("notice", "Day off added.");
                return "redirect:/master/days-off";
            } catch (IllegalArgumentException e) {
                result.rejectValue("day", "duplicate", e.getMessage());
            }
        }
        return page(model);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        daysOff.delete(id);
        redirect.addFlashAttribute("notice", "Day off removed.");
        return "redirect:/master/days-off";
    }

    private String page(Model model) {
        model.addAttribute("daysOff", daysOff.list());
        model.addAttribute("today", LocalDate.now());
        return "master/days-off";
    }
}
