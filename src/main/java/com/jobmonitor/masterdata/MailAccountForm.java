package com.jobmonitor.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class MailAccountForm {

    @NotBlank(message = "Enter the Gmail address.")
    @Pattern(regexp = "^\\s*[A-Za-z0-9.+_-]+@(?i:gmail\\.com|googlemail\\.com)\\s*$",
            message = "Enter a Gmail address, ending in @gmail.com.")
    private String emailAddress = "";

    /** Blank means keep the saved password. */
    private String password = "";

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
