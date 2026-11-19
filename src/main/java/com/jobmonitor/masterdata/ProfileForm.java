package com.jobmonitor.masterdata;

import jakarta.validation.constraints.Size;

public class ProfileForm {

    @Size(max = 120, message = "Keep the name under 120 characters.")
    private String fullName = "";

    @Size(max = 40, message = "Keep the phone number under 40 characters.")
    private String phone = "";

    @Size(max = 2000, message = "Keep the signature under 2,000 characters.")
    private String signature = "";

    public static ProfileForm from(Profile profile) {
        ProfileForm form = new ProfileForm();
        form.fullName = profile.getFullName();
        form.phone = profile.getPhone();
        form.signature = profile.getSignature();
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
