package com.jobmonitor.web;

import com.jobmonitor.masterdata.EmailTemplate;
import com.jobmonitor.masterdata.TemplateForm;
import com.jobmonitor.masterdata.TemplateService;
import jakarta.validation.Valid;
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
@RequestMapping("/master/templates")
public class TemplateController {

    private final TemplateService templates;

    public TemplateController(TemplateService templates) {
        this.templates = templates;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", templates.list());
        return "master/templates";
    }

    @GetMapping("/new")
    public String create(Model model) {
        TemplateForm form = new TemplateForm();
        templates.defaultTemplate().ifPresent(t -> {
            form.setSubject(t.getSubject());
            form.setBody(t.getBody());
        });
        return editor(form, model);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable long id, Model model) {
        return editor(TemplateForm.from(templates.get(id)), model);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") TemplateForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (!result.hasFieldErrors("name") && templates.nameTaken(form)) {
            result.rejectValue("name", "taken", "Another template already has this name.");
        }
        if (result.hasErrors()) {
            return editor(form, model);
        }
        EmailTemplate saved = templates.save(form);
        redirect.addFlashAttribute("notice", "Template \"" + saved.getName() + "\" saved.");
        return "redirect:/master/templates/" + saved.getId();
    }

    /** Live preview for the editor (htmx). */
    @PostMapping("/preview")
    public String preview(@ModelAttribute("form") TemplateForm form, BindingResult ignored, Model model) {
        model.addAttribute("rendered", templates.preview(form));
        return "master/template-preview :: preview";
    }

    @PostMapping("/{id}/default")
    public String makeDefault(@PathVariable long id, RedirectAttributes redirect) {
        templates.makeDefault(id);
        redirect.addFlashAttribute("notice", "Default template changed.");
        return "redirect:/master/templates";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirect) {
        try {
            templates.delete(id);
            redirect.addFlashAttribute("notice", "Template deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master/templates";
    }

    private String editor(TemplateForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("rendered", templates.preview(form));
        model.addAttribute("isDefault", form.getId() != null && templates.get(form.getId()).isDefaultTemplate());
        return "master/template-edit";
    }
}
