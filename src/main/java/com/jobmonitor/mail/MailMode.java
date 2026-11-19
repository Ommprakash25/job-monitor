package com.jobmonitor.mail;

public enum MailMode {
    /** The built-in fake mail server (GreenMail) on 127.0.0.1. Nothing leaves this computer. */
    FAKE,
    /** A real Gmail account. Arrives in phase 3. */
    GMAIL
}
