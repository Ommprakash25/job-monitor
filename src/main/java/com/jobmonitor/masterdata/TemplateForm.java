package com.jobmonitor.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TemplateForm {

    private Long id;

    @NotBlank(message = "Give the template a name.")
    @Size(max = 120, message = "Keep the name under 120 characters.")
    private String name = "";

    @NotBlank(message = "Write a subject.")
    @Size(max = 255, message = "Keep the subject under 255 characters.")
    private String subject = "";

    @NotBlank(message = "Write the email.")
    @Size(max = 10000, message = "Keep the email under 10,000 characters.")
    private String body = "";

    // Only used for the preview.
    private String sampleName = "Priya";
    private String sampleCompany = "Infosys";
    private String sampleRole = "";

    public static TemplateForm from(EmailTemplate template) {
        TemplateForm form = new TemplateForm();
        form.id = template.getId();
        form.name = template.getName();
        form.subject = template.getSubject();
        form.body = template.getBody();
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSampleCompany() {
        return sampleCompany;
    }

    public void setSampleCompany(String sampleCompany) {
        this.sampleCompany = sampleCompany;
    }

    public String getSampleRole() {
        return sampleRole;
    }

    public void setSampleRole(String sampleRole) {
        this.sampleRole = sampleRole;
    }
}
