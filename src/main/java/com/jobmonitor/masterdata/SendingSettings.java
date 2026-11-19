package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** When and how fast the app sends. There is always exactly one row. */
@Entity
@Table(name = "sending_settings")
public class SendingSettings {

    public static final int SINGLE_ID = 1;

    @Id
    private Integer id = SINGLE_ID;

    /** Comma-separated day names, for example MONDAY,TUESDAY. */
    @Column(name = "sending_days", nullable = false, length = 80)
    private String sendingDays;

    @Column(name = "window_start", nullable = false)
    private LocalTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalTime windowEnd;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "gap_min_minutes", nullable = false)
    private int gapMinMinutes;

    @Column(name = "gap_max_minutes", nullable = false)
    private int gapMaxMinutes;

    @Column(name = "daily_limit", nullable = false)
    private int dailyLimit;

    /** {@code null} means never email the same recruiter twice. */
    @Column(name = "recontact_after_days")
    private Integer recontactAfterDays;

    @Column(name = "company_limit_count", nullable = false)
    private int companyLimitCount;

    @Column(name = "company_limit_days", nullable = false)
    private int companyLimitDays;

    @Column(name = "follow_up_after_days", nullable = false)
    private int followUpAfterDays;

    /** {@code null} means the bounce pause is off. */
    @Column(name = "bounce_pause_percent")
    private Integer bouncePausePercent;

    @Column(name = "role_fallback", nullable = false, length = 120)
    private String roleFallback;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Set<DayOfWeek> getSendingDaySet() {
        if (sendingDays == null || sendingDays.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(sendingDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public void setSendingDaySet(Collection<DayOfWeek> days) {
        this.sendingDays = EnumSet.copyOf(days).stream().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    /** While true, nothing is sent. Starts paused; the user presses "Start sending". */
    @Column(name = "sending_paused", nullable = false)
    private boolean sendingPaused = true;

    /** The earliest moment the next email may go out (set after each send, and once per day at the start). */
    @Column(name = "next_send_at")
    private Instant nextSendAt;

    public boolean isSendingPaused() {
        return sendingPaused;
    }

    public void setSendingPaused(boolean sendingPaused) {
        this.sendingPaused = sendingPaused;
    }

    public Instant getNextSendAt() {
        return nextSendAt;
    }

    public void setNextSendAt(Instant nextSendAt) {
        this.nextSendAt = nextSendAt;
    }

    /** Why sending was paused automatically (bounce spike), or null. */
    @Column(name = "pause_reason", length = 300)
    private String pauseReason;

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public Integer getId() {
        return id;
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

    public int getGapMinMinutes() {
        return gapMinMinutes;
    }

    public void setGapMinMinutes(int gapMinMinutes) {
        this.gapMinMinutes = gapMinMinutes;
    }

    public int getGapMaxMinutes() {
        return gapMaxMinutes;
    }

    public void setGapMaxMinutes(int gapMaxMinutes) {
        this.gapMaxMinutes = gapMaxMinutes;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getRecontactAfterDays() {
        return recontactAfterDays;
    }

    public void setRecontactAfterDays(Integer recontactAfterDays) {
        this.recontactAfterDays = recontactAfterDays;
    }

    public int getCompanyLimitCount() {
        return companyLimitCount;
    }

    public void setCompanyLimitCount(int companyLimitCount) {
        this.companyLimitCount = companyLimitCount;
    }

    public int getCompanyLimitDays() {
        return companyLimitDays;
    }

    public void setCompanyLimitDays(int companyLimitDays) {
        this.companyLimitDays = companyLimitDays;
    }

    public int getFollowUpAfterDays() {
        return followUpAfterDays;
    }

    public void setFollowUpAfterDays(int followUpAfterDays) {
        this.followUpAfterDays = followUpAfterDays;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
