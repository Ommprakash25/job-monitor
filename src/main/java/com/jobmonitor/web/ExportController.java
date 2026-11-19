package com.jobmonitor.web;

import com.jobmonitor.ops.BackupService;
import com.jobmonitor.sending.EmailLog;
import com.jobmonitor.sending.EmailLogRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ExportController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EmailLogRepository logs;
    private final BackupService backups;

    public ExportController(EmailLogRepository logs, BackupService backups) {
        this.logs = logs;
        this.backups = backups;
    }

    /** The whole send log as an Excel file. */
    @GetMapping("/sent/export.xlsx")
    public ResponseEntity<byte[]> exportSent() throws IOException {
        List<EmailLog> all = logs.findAll(Sort.by(Sort.Direction.DESC, "queuedAt"));
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sent");
            String[] headers = {"Queued", "Sent", "Status", "To", "Company", "Subject", "Attachment",
                    "Template", "Channel", "Replied", "Bounced", "Note"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int r = 1;
            for (EmailLog l : all) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(l.getQueuedAt().atZone(ZONE).format(WHEN));
                row.createCell(1).setCellValue(l.getSentAt() == null ? "" : l.getSentAt().atZone(ZONE).format(WHEN));
                row.createCell(2).setCellValue(l.getStatus().name());
                row.createCell(3).setCellValue(l.getToAddress());
                row.createCell(4).setCellValue(l.getCompany().isOthers() ? "Others" : l.getCompany().getName());
                row.createCell(5).setCellValue(l.getSubject());
                row.createCell(6).setCellValue(l.getAttachmentName() == null ? "" : l.getAttachmentName());
                row.createCell(7).setCellValue(l.getTemplateName() == null ? "" : l.getTemplateName());
                row.createCell(8).setCellValue(l.getChannel());
                row.createCell(9).setCellValue(l.getRepliedAt() == null ? "" : l.getRepliedAt().atZone(ZONE).format(WHEN));
                row.createCell(10).setCellValue(l.getBouncedAt() == null ? "" : l.getBouncedAt().atZone(ZONE).format(WHEN));
                row.createCell(11).setCellValue(l.getError() == null ? "" : l.getError());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename("job-monitor-sent.xlsx", StandardCharsets.UTF_8).build().toString())
                    .body(out.toByteArray());
        }
    }

    @PostMapping("/backup/now")
    public String backupNow(RedirectAttributes redirect) {
        try {
            var file = backups.runBackup();
            redirect.addFlashAttribute("notice", "Backup saved to " + file + ".");
        } catch (IOException | InterruptedException | IllegalStateException e) {
            redirect.addFlashAttribute("error", "The backup didn't finish: " + e.getMessage());
        }
        return "redirect:/";
    }
}
