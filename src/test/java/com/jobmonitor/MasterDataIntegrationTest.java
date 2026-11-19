package com.jobmonitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobmonitor.masterdata.EmailTemplateRepository;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountRepository;
import com.jobmonitor.masterdata.ResumeRepository;
import com.jobmonitor.masterdata.SendingSettingsRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnabledIf(value = "com.jobmonitor.TestDatabase#isAvailable",
        disabledReason = "The test database isn't set up yet. Run db/create-database.sql and set JOB_MONITOR_DB_PASSWORD.")
class MasterDataIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired EmailTemplateRepository templates;
    @Autowired SendingSettingsRepository settings;
    @Autowired MailAccountRepository accounts;
    @Autowired ResumeRepository resumes;

    @Test
    void starterDataIsInPlace() {
        assertThat(templates.findFirstByDefaultTemplateTrue()).hasValueSatisfying(t ->
                assertThat(t.getName()).isEqualTo("Starter template"));
        assertThat(settings.findById(1)).hasValueSatisfying(s -> {
            assertThat(s.getGapMinMinutes()).isEqualTo(8);
            assertThat(s.getGapMaxMinutes()).isEqualTo(32);
            assertThat(s.getRoleFallback()).isEqualTo("Software Engineer");
        });
    }

    @Test
    void everyPageOpens() throws Exception {
        for (String path : List.of("/", "/master/profile", "/master/account", "/master/resumes",
                "/master/templates", "/master/templates/new", "/master/sending", "/master/days-off",
                "/master/free-mail", "/master/job-alerts")) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void savesValidSettingsAndRejectsGapMinAboveMax() throws Exception {
        mvc.perform(settingsPost(Map.of("gapMinMinutes", "40", "gapMaxMinutes", "10")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("minimum gap")));

        mvc.perform(settingsPost(Map.of("dailyLimit", "20")))
                .andExpect(status().is3xxRedirection());
        assertThat(settings.findById(1).orElseThrow().getDailyLimit()).isEqualTo(20);
    }

    @Test
    void keepsExactlyOneActiveResume() throws Exception {
        mvc.perform(multipart("/master/resumes").file(pdf("first.pdf"))).andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/master/resumes").file(pdf("second.pdf"))).andExpect(status().is3xxRedirection());
        Long second = resumes.findSummaries().stream().filter(r -> r.fileName().equals("second.pdf")).findFirst().orElseThrow().id();

        mvc.perform(post("/master/resumes/" + second + "/activate")).andExpect(status().is3xxRedirection());
        assertThat(resumes.findSummaries()).filteredOn(r -> r.active()).singleElement()
                .satisfies(r -> assertThat(r.fileName()).isEqualTo("second.pdf"));
    }

    @Test
    void storesTheAppPasswordEncryptedAndSignsInToTheFakeServer() throws Exception {
        mvc.perform(post("/master/account").param("emailAddress", "me@gmail.com").param("password", "abcd efgh ijkl mnop"))
                .andExpect(status().is3xxRedirection());
        MailAccount account = accounts.findFirstByActiveTrue().orElseThrow();
        assertThat(account.getSecretEncrypted()).startsWith("v1:").doesNotContain("abcdefghijklmnop");

        mvc.perform(post("/master/account/check")).andExpect(status().is3xxRedirection());
        assertThat(accounts.findFirstByActiveTrue().orElseThrow().getLastCheckOk()).isTrue();
    }

    @Test
    void refusesChangesFromOtherSites() throws Exception {
        mvc.perform(post("/master/profile").header("Origin", "https://evil.example").param("fullName", "x"))
                .andExpect(status().isForbidden());
    }

    /** A valid settings form, with some fields replaced. */
    private static MockHttpServletRequestBuilder settingsPost(Map<String, String> overrides) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("windowStart", "10:00");
        fields.put("windowEnd", "18:00");
        fields.put("timeZone", "Asia/Kolkata");
        fields.put("gapMinMinutes", "8");
        fields.put("gapMaxMinutes", "32");
        fields.put("dailyLimit", "15");
        fields.put("recontactAfterDays", "30");
        fields.put("companyLimitCount", "2");
        fields.put("companyLimitDays", "7");
        fields.put("followUpAfterDays", "7");
        fields.put("bouncePausePercent", "5");
        fields.put("roleFallback", "Software Engineer");
        fields.putAll(overrides);
        MockHttpServletRequestBuilder request = post("/master/sending")
                .param("sendingDays", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
        fields.forEach(request::param);
        return request;
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", "%PDF-1.4 test".getBytes());
    }
}
