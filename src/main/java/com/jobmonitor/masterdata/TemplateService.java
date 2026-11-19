package com.jobmonitor.masterdata;

import com.jobmonitor.NotFoundException;
import com.jobmonitor.template.PlaceholderValues;
import com.jobmonitor.template.RenderedEmail;
import com.jobmonitor.template.TemplateRenderer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TemplateService {

    private final EmailTemplateRepository templates;
    private final SendingSettingsService settings;
    private final TemplateRenderer renderer = new TemplateRenderer();

    public TemplateService(EmailTemplateRepository templates, SendingSettingsService settings) {
        this.templates = templates;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public List<EmailTemplate> list() {
        return templates.findAllByOrderByDefaultTemplateDescNameAsc();
    }

    @Transactional(readOnly = true)
    public EmailTemplate get(long id) {
        return templates.findById(id).orElseThrow(() -> new NotFoundException("That template doesn't exist."));
    }

    @Transactional(readOnly = true)
    public Optional<EmailTemplate> defaultTemplate() {
        return templates.findFirstByDefaultTemplateTrue();
    }

    @Transactional(readOnly = true)
    public boolean nameTaken(TemplateForm form) {
        String name = form.getName().strip();
        return form.getId() == null
                ? templates.existsByNameIgnoreCase(name)
                : templates.existsByNameIgnoreCaseAndIdNot(name, form.getId());
    }

    /** Creates or updates a template. The first template becomes the default. */
    public EmailTemplate save(TemplateForm form) {
        EmailTemplate template = form.getId() == null ? new EmailTemplate() : get(form.getId());
        template.setName(form.getName().strip());
        template.setSubject(form.getSubject().strip());
        template.setBody(form.getBody().strip());
        template.setUpdatedAt(Instant.now());
        if (template.getId() == null && templates.findFirstByDefaultTemplateTrue().isEmpty()) {
            template.setDefaultTemplate(true);
        }
        return templates.save(template);
    }

    public void makeDefault(long id) {
        get(id);
        templates.clearDefault();
        templates.makeDefault(id);
    }

    public void delete(long id) {
        EmailTemplate template = get(id);
        if (template.isDefaultTemplate()) {
            throw new IllegalStateException("This is the default template. Make another one the default before deleting it.");
        }
        templates.delete(template);
    }

    @Transactional(readOnly = true)
    public RenderedEmail preview(TemplateForm form) {
        PlaceholderValues values = new PlaceholderValues(form.getSampleName(), form.getSampleCompany(),
                form.getSampleRole(), settings.get().getRoleFallback());
        return renderer.render(form.getSubject(), form.getBody(), values);
    }

    /** True when the template still has [square brackets] to fill in. */
    @Transactional(readOnly = true)
    public boolean hasUnfilledDetails(EmailTemplate template) {
        return renderer.render(template.getSubject(), template.getBody(), new PlaceholderValues("", "", "", ""))
                .warnings().stream().anyMatch(w -> w.startsWith("Still to fill in"));
    }
}
