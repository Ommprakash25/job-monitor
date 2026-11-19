package com.jobmonitor.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Runs against the real fake mail server class, on its own ports. Nothing leaves the computer. */
class JavaMailGatewayTest {

    private static final int SMTP = 23025;
    private static final int IMAP = 23143;
    private static final FakeMailServer server = new FakeMailServer(SMTP, IMAP);

    private final JavaMailGateway gateway =
            new JavaMailGateway(MailMode.FAKE, MailServerSettings.fake(SMTP, IMAP), server);

    @BeforeAll
    static void start() {
        server.start();
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    @Test
    void signsInWithTheRightPassword() {
        ConnectionCheck result = gateway.checkConnection("me@gmail.com", "right-password");
        assertThat(result.ok()).as(result.message()).isTrue();
    }

    @Test
    void reportsAWrongPassword() {
        gateway.checkConnection("you@gmail.com", "right-password");
        JavaMailGateway withoutMailboxSetup = new JavaMailGateway(MailMode.FAKE, MailServerSettings.fake(SMTP, IMAP), null);
        ConnectionCheck result = withoutMailboxSetup.checkConnection("you@gmail.com", "wrong-password");
        assertThat(result.ok()).isFalse();
        assertThat(result.message()).contains("rejected");
    }

    @Test
    void testEmailArrivesWithoutTheComputerName() throws Exception {
        int before = server.receivedCount();
        gateway.sendTestEmail("sender@gmail.com", "Test Sender", "pw");

        MimeMessage[] received = server.greenMail().getReceivedMessages();
        assertThat(received).hasSize(before + 1);
        MimeMessage message = received[received.length - 1];
        assertThat(message.getSubject()).isEqualTo("job-monitor test email");
        assertThat(message.getMessageID()).endsWith("@gmail.com>");
    }
}
