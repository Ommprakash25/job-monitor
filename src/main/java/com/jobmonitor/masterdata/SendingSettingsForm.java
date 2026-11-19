package com.jobmonitor.masterdata;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.Errors;

public class SendingSettingsForm {

    @NotEmpty(message = "Pick at least one sending day.")
    private List<DayOfWeek> sendingDays = new ArrayList<>();

    @NotNull(message = "Set a start time.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime windowStart;

    @NotNull(message = "Set an end time.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime windowEnd;

    @NotBlank(message = "Pick a time zone.")
    private String timeZone;

    @NotNull(message = "Set the shortest gap.")
    @Min(value = 1, message = "The gap must be between 1 and 120 minutes.")
    @Max(value = 120, message = "The gap must be between 1 and 120 minutes.")
    private Integer gapMinMinutes;

    @NotNull(message = "Set the longest gap.")
    @Min(value = 1, message = "The gap must be between 1 and 120 minutes.")
    @Max(value = 120, message = "The gap must be between 1 and 120 minutes.")
    private Integer gapMaxMinutes;

    @NotNull(message = "Set the daily limit.")
    @Min(value = 1, message = "The daily limit must be between 1 and 500.")
    @Max(value = 500, message = "The daily limit must be between 1 and 500.")
    private Integer dailyLimit;

    private boolean recontactNever;

    private Integer recontactAfterDays;

    @NotNull(message = "Set how many recruiters per company.")
    @Min(value = 1, message = "Use 1 to 100 recruiters.")
    @Max(value = 100, message = "Use 1 to 100 recruiters.")
    private Integer companyLimitCount;

    @NotNull(message = "Set the number of days.")
    @Min(value = 1, message = "Use 1 to 365 days.")
    @Max(value = 365, message = "Use 1 to 365 days.")
    private Integer companyLimitDays;

    @NotNull(message = "Set the number of days.")
    @Min(value = 1, message = "Use 1 to 60 days.")
    @Max(value = 60, message = "Use 1 to 60 days.")
    private Integer followUpAfterDays;

    private boolean bouncePauseOff;

    private Integer bouncePausePercent;

    @NotBlank(message = "Set a fallback role, for example Software Engineer.")
    @Size(max = 120, message = "Keep the role under 120 characters.")
    private String roleFallback;

    public static SendingSettingsForm from(SendingSettings settings) {
        SendingSettingsForm form = new SendingSettingsForm();
        form.sendingDays = new ArrayList<>(settings.getSendingDaySet());
        form.windowStart = settings.getWindowStart();
        form.windowEnd = settings.getWindowEnd();
        form.timeZone = settings.getTimeZone();
        form.gapMinMinutes = settings.getGapMinMinutes();
        form.gapMaxMinutes = settings.getGapMaxMinutes();
        form.dailyLimit = settings.getDailyLimit();
        form.recontactNever = settings.getRecontactAfterDays() == null;
        form.recontactAfterDays = settings.getRecontactAfterDays() == null ? 30 : settings.getRecontactAfterDays();
        form.companyLimitCount = settings.getCompanyLimitCount();
        form.companyLimitDays = settings.getCompanyLimitDays();
        form.followUpAfterDays = settings.getFollowUpAfterDays();
        form.bouncePauseOff = settings.getBouncePausePercent() == null;
        form.bouncePausePercent = settings.getBouncePausePercent() == null ? 5 : settings.getBouncePausePercent();
        form.roleFallback = settings.getRoleFallback();
        return form;
    }

    /** Checks that need more than one field. */
    public void checkCombinations(Errors errors) {
        if (gapMinMinutes != null && gapMaxMinutes != null && gapMinMinutes > gapMaxMinutes) {
            errors.rejectValue("gapMinMinutes", "gap.order", "The minimum gap can't be more than the maximum.");
        }
        if (windowStart != null && windowEnd != null && !windowStart.isBefore(windowEnd)) {
            errors.rejectValue("windowEnd", "window.order", "The end time must be after the start time.");
        }
        if (timeZone != null && !timeZone.isBlank()) {
            try {
                ZoneId.of(timeZone);
            } catch (DateTimeException e) {
                errors.rejectValue("timeZone", "zone", "Pick a time zone from the list.");
            }
        }
        if (!recontactNever && (recontactAfterDays == null || recontactAfterDays < 1 || recontactAfterDays > 3650)) {
            errors.rejectValue("recontactAfterDays", "recontact", "Enter 1 to 3,650 days, or tick \"Never\".");
        }
        if (!bouncePauseOff && (bouncePausePercent == null || bouncePausePercent < 1 || bouncePausePercent > 50)) {
            errors.rejectValue("bouncePausePercent", "bounce", "Enter 1 to 50%, or turn the pause off.");
        }
    }

    public void applyTo(SendingSettings settings) {
        settings.setSendingDaySet(sendingDays);
        settings.setWindowStart(windowStart);
        settings.setWindowEnd(windowEnd);
        settings.setTimeZone(timeZone);
        settings.setGapMinMinutes(gapMinMinutes);
        settings.setGapMaxMinutes(gapMaxMinutes);
        settings.setDailyLimit(dailyLimit);
        settings.setRecontactAfterDays(recontactNever ? null : recontactAfterDays);
        settings.setCompanyLimitCount(companyLimitCount);
        settings.setCompanyLimitDays(companyLimitDays);
        settings.setFollowUpAfterDays(followUpAfterDays);
        settings.setBouncePausePercent(bouncePauseOff ? null : bouncePausePercent);
        settings.setRoleFallback(roleFallback.strip());
    }

    public SendingCapacity capacity() {
        return SendingCapacity.estimate(windowStart, windowEnd, gapMinMinutes, gapMaxMinutes, dailyLimit);
    }

    public List<DayOfWeek> getSendingDays() {
        return sendingDays;
    }

    public void setSendingDays(List<DayOfWeek> sendingDays) {
        this.sendingDays = sendingDays;
    }

    public LocalTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Integer getGapMinMinutes() {
        return gapMinMinutes;
    }

    public void setGapMinMinutes(Integer gapMinMinutes) {
        this.gapMinMinutes = gapMinMinutes;
    }

    public Integer getGapMaxMinutes() {
        return gapMaxMinutes;
    }

    public void setGapMaxMinutes(Integer gapMaxMinutes) {
        this.gapMaxMinutes = gapMaxMinutes;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public boolean isRecontactNever() {
        return recontactNever;
    }

    public void setRecontactNever(boolean recontactNever) {
        this.recontactNever = recontactNever;
    }

    public Integer getRecontactAfterDays() {
        return recontactAfterDays;
    }

    public void setRecontactAfterDays(Integer recontactAfterDays) {
        this.recontactAfterDays = recontactAfterDays;
    }

    public Integer getCompanyLimitCount() {
        return companyLimitCount;
    }

    public void setCompanyLimitCount(Integer companyLimitCount) {
        this.companyLimitCount = companyLimitCount;
    }

    public Integer getCompanyLimitDays() {
        return companyLimitDays;
    }

    public void setCompanyLimitDays(Integer companyLimitDays) {
        this.companyLimitDays = companyLimitDays;
    }

    public Integer getFollowUpAfterDays() {
        return followUpAfterDays;
    }

    public void setFollowUpAfterDays(Integer followUpAfterDays) {
        this.followUpAfterDays = followUpAfterDays;
    }

    public boolean isBouncePauseOff() {
        return bouncePauseOff;
    }

    public void setBouncePauseOff(boolean bouncePauseOff) {
        this.bouncePauseOff = bouncePauseOff;
    }

    public Integer getBouncePausePercent() {
        return bouncePausePercent;
    }

    public void setBouncePausePercent(Integer bouncePausePercent) {
        this.bouncePausePercent = bouncePausePercent;
    }

    public String getRoleFallback() {
        return roleFallback;
    }

    public void setRoleFallback(String roleFallback) {
        this.roleFallback = roleFallback;
    }
}
