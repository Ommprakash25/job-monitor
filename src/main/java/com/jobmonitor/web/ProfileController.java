package com.jobmonitor.web;

import com.jobmonitor.masterdata.ProfileForm;
import com.jobmonitor.masterdata.ProfileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/profile")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public String show(Model model) {
        model.addAttribute("form", ProfileForm.from(profiles.get()));
        return "master/profile";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "master/profile";
        }
        profiles.save(form);
        redirect.addFlashAttribute("notice", "Profile saved.");
        return "redirect:/master/profile";
    }
}
