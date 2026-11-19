package com.jobmonitor.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reads new mail every 5 minutes, once the Gmail account is connected. */
@Component
public class InboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(InboxScheduler.class);

    private final InboxSyncService sync;

    public InboxScheduler(InboxSyncService sync) {
        this.sync = sync;
    }

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void tick() {
        if (!sync.isConfigured()) {
            return;
        }
        try {
            InboxSyncService.SyncResult result = sync.sync();
            log.info("Inbox sync: {} new, {} linked, {} bounces", result.added(), result.linked(), result.bounces());
        } catch (RuntimeException e) {
            log.warn("Inbox sync failed: {}", e.getMessage());
        }
    }
}
