package com.jobmonitor.masterdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

class SendingSettingsTest {

    @Test
    void defaultSettingsFitAbout24EmailsAndReach15Around1455() {
        SendingCapacity capacity = SendingCapacity.estimate(LocalTime.of(10, 0), LocalTime.of(18, 0), 8, 32, 15);
        assertThat(capacity.emailsThatFit()).isEqualTo(24);
        assertThat(capacity.summary()).contains("About 24 emails fit").contains("around 14:55");
    }

    @Test
    void saysWhenTheLimitWontBeReached() {
        SendingCapacity capacity = SendingCapacity.estimate(LocalTime.of(10, 0), LocalTime.of(18, 0), 8, 32, 40);
        assertThat(capacity.summary()).contains("a limit of 40 won't be reached");
    }

    @Test
    void handlesHalfTypedValues() {
        assertThat(SendingCapacity.estimate(null, LocalTime.NOON, 8, 32, 15).emailsThatFit()).isZero();
        assertThat(SendingCapacity.estimate(LocalTime.NOON, LocalTime.of(10, 0), 8, 32, 15).emailsThatFit()).isZero();
    }

    @Test
    void validFormHasNoCombinationErrors() {
        assertThat(check(validForm()).hasErrors()).isFalse();
    }

    @Test
    void rejectsMinimumGapAboveMaximum() {
        SendingSettingsForm form = validForm();
        form.setGapMinMinutes(40);
        form.setGapMaxMinutes(10);
        Errors errors = check(form);
        assertThat(errors.getFieldError("gapMinMinutes").getDefaultMessage())
                .isEqualTo("The minimum gap can't be more than the maximum.");
    }

    @Test
    void rejectsEndBeforeStartAndBadTimeZone() {
        SendingSettingsForm form = validForm();
        form.setWindowEnd(LocalTime.of(9, 0));
        form.setTimeZone("Mars/Base");
        Errors errors = check(form);
        assertThat(errors.hasFieldErrors("windowEnd")).isTrue();
        assertThat(errors.hasFieldErrors("timeZone")).isTrue();
    }

    @Test
    void neverRecontactNeedsNoDayCount() {
        SendingSettingsForm form = validForm();
        form.setRecontactNever(true);
        form.setRecontactAfterDays(null);
        assertThat(check(form).hasErrors()).isFalse();

        SendingSettings settings = new SendingSettings();
        form.applyTo(settings);
        assertThat(settings.getRecontactAfterDays()).isNull();
        assertThat(settings.getSendingDaySet()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    private static Errors check(SendingSettingsForm form) {
        Errors errors = new BeanPropertyBindingResult(form, "form");
        form.checkCombinations(errors);
        return errors;
    }

    private static SendingSettingsForm validForm() {
        SendingSettingsForm form = new SendingSettingsForm();
        form.setSendingDays(List.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY));
        form.setWindowStart(LocalTime.of(10, 0));
        form.setWindowEnd(LocalTime.of(18, 0));
        form.setTimeZone("Asia/Kolkata");
        form.setGapMinMinutes(8);
        form.setGapMaxMinutes(32);
        form.setDailyLimit(15);
        form.setRecontactAfterDays(30);
        form.setCompanyLimitCount(2);
        form.setCompanyLimitDays(7);
        form.setFollowUpAfterDays(7);
        form.setBouncePausePercent(5);
        form.setRoleFallback("Software Engineer");
        return form;
    }
}
