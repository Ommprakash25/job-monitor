package com.jobmonitor.sending;

import com.jobmonitor.NotFoundException;
import com.jobmonitor.masterdata.FreeMailDomainRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Recruiters and companies: reading pasted lists, adding recruiters, and choosing companies. */
@Service
@Transactional
public class RecruiterService {

    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9][a-z0-9._+-]*@(?:[a-z0-9-]+\\.)+[a-z]{2,63}$");
    private static final Pattern DOMAIN = Pattern.compile("^(?:[a-z0-9-]+\\.)+[a-z]{2,63}$");
    private static final Set<String> SECOND_LEVEL = Set.of("co", "com", "org", "net", "ac", "gov", "edu");

    public record Entry(String email, String name) {
    }

    /** Lines that parsed, and lines that didn't. */
    public record Parsed(List<Entry> valid, List<String> invalid) {
    }

    public record ImportResult(int added, int alreadyKnown, int repeated) {
    }

    private final RecruiterRepository recruiters;
    private final CompanyRepository companies;
    private final FreeMailDomainRepository freeMailDomains;

    public RecruiterService(RecruiterRepository recruiters, CompanyRepository companies,
                            FreeMailDomainRepository freeMailDomains) {
        this.recruiters = recruiters;
        this.companies = companies;
        this.freeMailDomains = freeMailDomains;
    }

    /** One recruiter per line: an address, optionally followed by a comma and a name. */
    public static Parsed parse(String text) {
        List<Entry> valid = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        if (text == null) {
            return new Parsed(valid, invalid);
        }
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("[,\\t]", 2);
            String email = parts[0].strip().toLowerCase(Locale.ROOT);
            if (!EMAIL.matcher(email).matches()) {
                invalid.add(line.strip());
                continue;
            }
            valid.add(new Entry(email, parts.length > 1 ? parts[1].strip() : ""));
        }
        return new Parsed(valid, invalid);
    }

    public ImportResult importEntries(List<Entry> entries, String source) {
        Map<String, Entry> unique = new LinkedHashMap<>();
        int repeated = 0;
        for (Entry entry : entries) {
            if (unique.putIfAbsent(entry.email(), entry) != null) {
                repeated++;
            }
        }
        int added = 0;
        int alreadyKnown = 0;
        for (Entry entry : unique.values()) {
            if (recruiters.existsByEmail(entry.email())) {
                alreadyKnown++;
            } else {
                create(entry, source);
                added++;
            }
        }
        return new ImportResult(added, alreadyKnown, repeated);
    }

    public Recruiter findOrCreate(Entry entry, String source) {
        return recruiters.findByEmail(entry.email()).orElseGet(() -> create(entry, source));
    }

    /** Changes a recruiter's name, and optionally their company (by domain). */
    public void update(long id, String name, String companyDomain) {
        Recruiter recruiter = recruiters.findById(id)
                .orElseThrow(() -> new NotFoundException("That recruiter doesn't exist."));
        recruiter.setName(name == null ? "" : name.strip());
        String domain = companyDomain == null ? "" : companyDomain.strip().toLowerCase(Locale.ROOT);
        domain = domain.replaceFirst("^@", "");
        if (!domain.isEmpty()) {
            if (!DOMAIN.matcher(domain).matches()) {
                throw new IllegalArgumentException("Enter a company domain like infosys.com, or leave it blank.");
            }
            recruiter.setCompany(companyFor("x@" + domain));
        }
    }

    public void setDoNotContact(long id, boolean value) {
        Recruiter recruiter = recruiters.findById(id)
                .orElseThrow(() -> new NotFoundException("That recruiter doesn't exist."));
        recruiter.setDoNotContact(value);
    }

    private Recruiter create(Entry entry, String source) {
        String name = entry.name().isBlank() ? NameGuesser.guess(entry.email()) : entry.name();
        return recruiters.save(new Recruiter(entry.email(), name, companyFor(entry.email()), source));
    }

    Company companyFor(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        if (freeMailDomains.existsById(domain)) {
            return others();
        }
        return companies.findByDomain(domain)
                .orElseGet(() -> companies.save(new Company(companyName(domain), domain)));
    }

    private Company others() {
        return companies.findFirstByDomainIsNull()
                .orElseThrow(() -> new IllegalStateException("The Others company is missing. Run the database setup again."));
    }

    /** "infosys.com" becomes "Infosys"; "ipsl.co.in" becomes "Ipsl". Editable later. */
    static String companyName(String domain) {
        String[] labels = domain.split("\\.");
        int index = labels.length - 2;
        if (index > 0 && SECOND_LEVEL.contains(labels[index])) {
            index--;
        }
        return NameGuesser.capitalize(labels[Math.max(index, 0)]);
    }
}
