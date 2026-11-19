package com.jobmonitor.web;

import com.jobmonitor.masterdata.SendingSettingsForm;
import com.jobmonitor.masterdata.SendingSettingsService;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/sending")
public class SendingSettingsController {

    private static final List<String> TIME_ZONES = ZoneId.getAvailableZoneIds().stream()
            .filter(id -> id.contains("/") && !id.startsWith("Etc/") && !id.startsWith("SystemV/"))
            .sorted()
            .toList();

    private final SendingSettingsService settings;

    public SendingSettingsController(SendingSettingsService settings) {
        this.settings = settings;
    }

    @ModelAttribute("timeZones")
    public List<String> timeZones() {
        return TIME_ZONES;
    }

    @ModelAttribute("weekDays")
    public DayOfWeek[] weekDays() {
        return DayOfWeek.values();
    }

    @GetMapping
    public String show(Model model) {
        SendingSettingsForm form = SendingSettingsForm.from(settings.get());
        model.addAttribute("form", form);
        model.addAttribute("capacity", form.capacity());
        return "master/sending";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") SendingSettingsForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        form.checkCombinations(result);
        if (result.hasErrors()) {
            model.addAttribute("capacity", form.capacity());
            return "master/sending";
        }
        settings.save(form);
        redirect.addFlashAttribute("notice", "Sending settings saved. They apply from the next email.");
        return "redirect:/master/sending";
    }

    /** Live "how many emails fit" line (htmx). Half-typed values are fine here. */
    @PostMapping("/estimate")
    public String estimate(@ModelAttribute("form") SendingSettingsForm form, BindingResult ignored, Model model) {
        model.addAttribute("capacity", form.capacity());
        return "master/sending :: capacity";
    }
}
