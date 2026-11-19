package com.jobmonitor.masterdata;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The free-mail domain list and the job-alert sender list in Master data. */
@Service
@Transactional
public class DomainListService {

    private static final String LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";
    static final Pattern DOMAIN = Pattern.compile("^(?=.{1,253}$)(?:" + LABEL + "\\.)+[a-z]{2,63}$");
    static final Pattern ADDRESS = Pattern.compile("^[a-z0-9.+_-]+@(?:" + LABEL + "\\.)+[a-z]{2,63}$");

    private final FreeMailDomainRepository freeMailDomains;
    private final JobAlertSenderRepository jobAlertSenders;

    public DomainListService(FreeMailDomainRepository freeMailDomains, JobAlertSenderRepository jobAlertSenders) {
        this.freeMailDomains = freeMailDomains;
        this.jobAlertSenders = jobAlertSenders;
    }

    @Transactional(readOnly = true)
    public List<String> freeMailDomains() {
        return freeMailDomains.findAllByOrderByDomainAsc().stream().map(FreeMailDomain::getDomain).toList();
    }

    public String addFreeMailDomain(String raw) {
        String domain = normalize(raw);
        if (!DOMAIN.matcher(domain).matches()) {
            throw new IllegalArgumentException("Enter a domain like gmail.com.");
        }
        if (freeMailDomains.existsById(domain)) {
            throw new IllegalArgumentException(domain + " is already on the list.");
        }
        freeMailDomains.save(new FreeMailDomain(domain));
        return domain;
    }

    public void removeFreeMailDomain(String domain) {
        freeMailDomains.deleteById(normalize(domain));
    }

    @Transactional(readOnly = true)
    public List<String> jobAlertSenders() {
        return jobAlertSenders.findAllByOrderByPatternAsc().stream().map(JobAlertSender::getPattern).toList();
    }

    public String addJobAlertSender(String raw) {
        String pattern = normalize(raw);
        if (!DOMAIN.matcher(pattern).matches() && !ADDRESS.matcher(pattern).matches()) {
            throw new IllegalArgumentException("Enter a domain like naukri.com, or a full email address.");
        }
        if (jobAlertSenders.existsById(pattern)) {
            throw new IllegalArgumentException(pattern + " is already on the list.");
        }
        jobAlertSenders.save(new JobAlertSender(pattern));
        return pattern;
    }

    public void removeJobAlertSender(String pattern) {
        jobAlertSenders.deleteById(normalize(pattern));
    }

    static String normalize(String raw) {
        String value = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
        value = value.replaceFirst("^https?://", "");
        if (value.startsWith("@")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
