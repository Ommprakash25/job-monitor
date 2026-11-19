package com.jobmonitor.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pulls new job postings from Adzuna's public job search API, once a day and on demand.
 * Needs a free Adzuna app ID and key (developer.adzuna.com), set in application.properties.
 */
@Service
@Transactional
public class JobFeedService {

    private static final Logger log = LoggerFactory.getLogger(JobFeedService.class);
    private static final String SOURCE = "adzuna";

    private final JobListingRepository jobs;
    private final ObjectMapper mapper;
    private final String appId;
    private final String appKey;
    private final String country;
    private final String search;
    private final String location;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public JobFeedService(JobListingRepository jobs, ObjectMapper mapper,
                          @Value("${jobmonitor.jobs.adzuna-app-id:}") String appId,
                          @Value("${jobmonitor.jobs.adzuna-app-key:}") String appKey,
                          @Value("${jobmonitor.jobs.country:in}") String country,
                          @Value("${jobmonitor.jobs.search:java backend developer}") String search,
                          @Value("${jobmonitor.jobs.location:}") String location) {
        this.jobs = jobs;
        this.mapper = mapper;
        this.appId = appId;
        this.appKey = appKey;
        this.country = country;
        this.search = search;
        this.location = location;
    }

    public boolean isConfigured() {
        return !appId.isBlank() && !appKey.isBlank();
    }

    /** Fetches the newest postings from the last day. Returns how many were new. */
    public int refresh() {
        if (!isConfigured()) {
            throw new IllegalStateException("Add your Adzuna app ID and key to application.properties "
                    + "(jobmonitor.jobs.adzuna-app-id and jobmonitor.jobs.adzuna-app-key). "
                    + "They're free at developer.adzuna.com.");
        }
        StringBuilder url = new StringBuilder("https://api.adzuna.com/v1/api/jobs/" + country + "/search/1")
                .append("?app_id=").append(enc(appId))
                .append("&app_key=").append(enc(appKey))
                .append("&what=").append(enc(search))
                .append("&max_days_old=1&sort_by=date&results_per_page=50");
        if (!location.isBlank()) {
            url.append("&where=").append(enc(location));
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Adzuna answered " + response.statusCode()
                        + ". Check the app ID and key.");
            }
            JsonNode results = mapper.readTree(response.body()).path("results");
            int added = 0;
            for (JsonNode result : results) {
                String externalId = result.path("id").asText("");
                if (externalId.isEmpty()) {
                    continue;
                }
                JobListing job = jobs.findBySourceAndExternalId(SOURCE, externalId).orElse(null);
                if (job == null) {
                    job = new JobListing(SOURCE, externalId);
                    jobs.save(job);
                    added++;
                }
                job.update(result.path("title").asText(""), result.path("company").path("display_name").asText(""),
                        result.path("location").path("display_name").asText(""),
                        result.path("redirect_url").asText(""), parseInstant(result.path("created").asText("")),
                        result.path("description").asText(""));
            }
            return added;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Couldn't reach Adzuna: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The job search was interrupted.", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void daily() {
        if (!isConfigured()) {
            return;
        }
        try {
            log.info("Job feed: {} new postings", refresh());
        } catch (RuntimeException e) {
            log.warn("Job feed failed: {}", e.getMessage());
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Instant parseInstant(String value) {
        try {
            return value.isEmpty() ? null : Instant.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
