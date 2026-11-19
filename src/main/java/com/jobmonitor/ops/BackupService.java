package com.jobmonitor.ops;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Saves a copy of the whole database every night with PostgreSQL's pg_dump, and keeps the
 * newest 14 copies. Mailbox passwords stay encrypted in the copy; the key is in your home folder.
 */
@Component
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final int KEEP = 14;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern JDBC_URL = Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    private final String backupDir;
    private final String pgDump;
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public BackupService(
            @Value("${jobmonitor.backup.dir:${user.home}/.job-monitor/backups}") String backupDir,
            @Value("${jobmonitor.backup.pg-dump:C:/Program Files/PostgreSQL/18/bin/pg_dump.exe}") String pgDump,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String user,
            @Value("${spring.datasource.password:}") String password) {
        this.backupDir = backupDir;
        this.pgDump = pgDump;
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    @Scheduled(cron = "0 30 23 * * *")
    public void nightly() {
        try {
            Path file = runBackup();
            log.info("Nightly backup saved to {}", file);
        } catch (IOException | InterruptedException | RuntimeException e) {
            log.warn("Nightly backup failed: {}", e.getMessage());
        }
    }

    public Path runBackup() throws IOException, InterruptedException {
        Matcher url = JDBC_URL.matcher(jdbcUrl);
        if (!url.find()) {
            throw new IllegalStateException("Couldn't read the database address from the settings.");
        }
        Path folder = Path.of(backupDir);
        Files.createDirectories(folder);
        Path file = folder.resolve("job-monitor-" + LocalDateTime.now().format(STAMP) + ".dump");

        ProcessBuilder builder = new ProcessBuilder(pgDump, "-h", url.group(1), "-p", url.group(2),
                "-U", user, "-F", "c", "-f", file.toString(), url.group(3));
        builder.environment().put("PGPASSWORD", password);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            Files.deleteIfExists(file);
            throw new IllegalStateException("The backup didn't finish: " + output.strip());
        }
        prune(folder);
        return file;
    }

    private static void prune(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            List<Path> dumps = files.filter(p -> p.getFileName().toString().startsWith("job-monitor-")
                            && p.getFileName().toString().endsWith(".dump"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
            for (int i = KEEP; i < dumps.size(); i++) {
                Files.deleteIfExists(dumps.get(i));
            }
        }
    }
}
