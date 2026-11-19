package com.jobmonitor.inbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InboxRulesTest {

    @Test
    void findsEveryMessageIdInHeaderText() {
        assertThat(InboxSyncService.messageIds("<abc.1@gmail.com> <def@mail.example.org>"))
                .containsExactly("<abc.1@gmail.com>", "<def@mail.example.org>");
    }

    @Test
    void ignoresTextWithoutMessageIds() {
        assertThat(InboxSyncService.messageIds("no ids here")).isEmpty();
        assertThat(InboxSyncService.messageIds(null)).isEmpty();
    }

    @Test
    void recognisesBounces() {
        assertThat(InboxSyncService.isBounce("Mailer-Daemon@googlemail.com", "Delivery Status Notification (Failure)")).isTrue();
        assertThat(InboxSyncService.isBounce("postmaster@example.com", "anything")).isTrue();
        assertThat(InboxSyncService.isBounce("recruiter@infosys.com", "Undelivered Mail Returned to Sender")).isTrue();
    }

    @Test
    void ordinaryMailIsNotABounce() {
        assertThat(InboxSyncService.isBounce("priya@infosys.com", "Re: Java developer role")).isFalse();
        assertThat(InboxSyncService.isBounce("alerts@naukri.com", "5 new jobs for you")).isFalse();
    }
}
