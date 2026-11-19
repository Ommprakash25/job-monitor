package com.jobmonitor.masterdata;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SendingSettingsService {

    private final SendingSettingsRepository repository;

    public SendingSettingsService(SendingSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SendingSettings get() {
        return repository.findById(SendingSettings.SINGLE_ID)
                .orElseThrow(() -> new IllegalStateException("The sending settings row is missing. Did the database setup finish?"));
    }

    public void setPaused(boolean paused) {
        SendingSettings settings = get();
        settings.setSendingPaused(paused);
        if (!paused) {
            settings.setPauseReason(null);
        }
    }

    public void pauseAutomatically(String reason) {
        SendingSettings settings = get();
        settings.setSendingPaused(true);
        settings.setPauseReason(reason);
    }

    public void save(SendingSettingsForm form) {
        SendingSettings settings = get();
        form.applyTo(settings);
        settings.setUpdatedAt(Instant.now());
    }
}
