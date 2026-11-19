package com.jobmonitor.mail;

import com.jobmonitor.config.JobMonitorProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(name = "jobmonitor.mail.mode", havingValue = "fake", matchIfMissing = true)
    FakeMailServer fakeMailServer(JobMonitorProperties properties) {
        return new FakeMailServer(properties.mail().fakeSmtpPort(), properties.mail().fakeImapPort());
    }

    @Bean
    MailGateway mailGateway(JobMonitorProperties properties, ObjectProvider<FakeMailServer> fakeMailServer) {
        JobMonitorProperties.Mail mail = properties.mail();
        if (mail.mode() == MailMode.GMAIL) {
            // Real Gmail over SMTP (sending) and IMAP (reading), signed in with an app password.
            return new JavaMailGateway(MailMode.GMAIL, MailServerSettings.gmail(), null);
        }
        return new JavaMailGateway(MailMode.FAKE,
                MailServerSettings.fake(mail.fakeSmtpPort(), mail.fakeImapPort()),
                fakeMailServer.getObject());
    }
}
