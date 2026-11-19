package com.jobmonitor.config;

import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Opens the app in the default browser once it's ready. start.bat turns this on. */
@Component
@ConditionalOnProperty(name = "jobmonitor.open-browser", havingValue = "true")
public class OpenBrowserOnStartup {

    private static final Logger log = LoggerFactory.getLogger(OpenBrowserOnStartup.class);

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        String port = event.getApplicationContext().getEnvironment().getProperty("local.server.port", "8080");
        String url = "http://127.0.0.1:" + port + "/";
        log.info("job-monitor is ready at {}", url);
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            return;
        }
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (IOException e) {
            log.warn("Couldn't open the browser. Open {} yourself.", url);
        }
    }
}
