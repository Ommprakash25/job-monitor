package com.jobmonitor.config;

import com.jobmonitor.mail.MailMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Settings under {@code jobmonitor.*} in application.properties. */
@ConfigurationProperties(prefix = "jobmonitor")
public record JobMonitorProperties(
        @DefaultValue Mail mail,
        @DefaultValue Security security,
        @DefaultValue("false") boolean openBrowser) {

    public record Mail(
            @DefaultValue("fake") MailMode mode,
            @DefaultValue("3025") int fakeSmtpPort,
            @DefaultValue("3143") int fakeImapPort) {
    }

    /**
     * @param keyFile where the encryption key lives; created on first start
     * @param key     optional key given directly (base64), for example through the
     *                JOBMONITOR_SECURITY_KEY environment variable; wins over the file
     */
    public record Security(String keyFile, String key) {
    }
}
