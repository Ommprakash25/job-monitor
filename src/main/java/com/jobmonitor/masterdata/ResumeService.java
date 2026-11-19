package com.jobmonitor.masterdata;

import com.jobmonitor.NotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ResumeService {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ResumeRepository resumes;

    public ResumeService(ResumeRepository resumes) {
        this.resumes = resumes;
    }

    @Transactional(readOnly = true)
    public List<ResumeSummary> list() {
        return resumes.findSummaries();
    }

    @Transactional(readOnly = true)
    public boolean hasActive() {
        return resumes.existsByActiveTrue();
    }

    /** The first resume you upload becomes the active one. */
    public Resume upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a file to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("That file is over 10 MB. Upload a smaller one.");
        }
        String name = cleanFileName(file.getOriginalFilename());
        String contentType = CONTENT_TYPES.get(extension(name));
        if (contentType == null) {
            throw new IllegalArgumentException("Upload a PDF or Word file (.pdf, .doc or .docx). PDF works best.");
        }
        boolean first = !resumes.existsByActiveTrue();
        return resumes.save(new Resume(name, contentType, file.getBytes(), first));
    }

    public void activate(long id) {
        if (!resumes.existsById(id)) {
            throw new NotFoundException("That resume doesn't exist.");
        }
        resumes.deactivateAll();
        resumes.activate(id);
    }

    @Transactional(readOnly = true)
    public Resume get(long id) {
        return resumes.findById(id).orElseThrow(() -> new NotFoundException("That resume doesn't exist."));
    }

    public void delete(long id) {
        Resume resume = get(id);
        if (resume.isActive()) {
            throw new IllegalStateException("This is the active resume. Make another one active before deleting it.");
        }
        resumes.delete(resume);
    }

    static String cleanFileName(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^A-Za-z0-9._ ()-]", "_").strip();
        if (name.isEmpty()) {
            name = "resume";
        }
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name;
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
