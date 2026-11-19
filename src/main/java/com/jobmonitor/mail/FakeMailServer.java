package com.jobmonitor.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * GreenMail running inside the app on 127.0.0.1. It accepts email the way Gmail would
 * (SMTP for sending, IMAP for reading) but delivers nothing anywhere.
 */
public class FakeMailServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FakeMailServer.class);

    private final GreenMail greenMail;
    private final int smtpPort;
    private final int imapPort;
    private volatile boolean running;

    public FakeMailServer(int smtpPort, int imapPort) {
        this.smtpPort = smtpPort;
        this.imapPort = imapPort;
        this.greenMail = new GreenMail(new ServerSetup[] {
                new ServerSetup(smtpPort, "127.0.0.1", ServerSetup.PROTOCOL_SMTP),
                new ServerSetup(imapPort, "127.0.0.1", ServerSetup.PROTOCOL_IMAP)});
    }

    @Override
    public void start() {
        greenMail.start();
        running = true;
        log.info("Fake mail server running on 127.0.0.1 (SMTP port {}, IMAP port {}). Nothing leaves this computer.",
                smtpPort, imapPort);
    }

    @Override
    public void stop() {
        greenMail.stop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Creates the mailbox on the fake server, or updates its password. */
    public void ensureUser(String emailAddress, String password) {
        greenMail.setUser(emailAddress, emailAddress, password);
    }

    public int receivedCount() {
        return greenMail.getReceivedMessages().length;
    }

    public GreenMail greenMail() {
        return greenMail;
    }
}
