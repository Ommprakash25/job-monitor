package com.jobmonitor.sending;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads a recruiter list from an Excel (.xlsx) or CSV file. The first row holds the column
 * names. Only "email" is required; "name" and "role" are used when present, and "company"
 * is shown but the company is always taken from the email domain.
 */
public final class UploadParser {

    private UploadParser() {
    }

    public static List<UploadRow> parse(String fileName, InputStream in) throws IOException {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx")) {
            return parseExcel(in);
        }
        if (lower.endsWith(".csv")) {
            return parseCsv(in);
        }
        throw new IllegalArgumentException("Upload an Excel file (.xlsx) or a CSV file (.csv).");
    }

    static List<UploadRow> parseExcel(InputStream in) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<List<String>> table = new ArrayList<>();
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (int i = 0; i < row.getLastCellNum(); i++) {
                    cells.add(formatter.formatCellValue(row.getCell(i)));
                }
                table.add(cells);
            }
            return fromTable(table);
        }
    }

    static List<UploadRow> parseCsv(InputStream in) throws IOException {
        List<List<String>> table = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    line = line.replace("﻿", "");
                    first = false;
                }
                // Blank lines stay in the count, so row numbers match the file; they're skipped later.
                table.add(line.isBlank() ? List.of() : splitCsv(line));
            }
        }
        return fromTable(table);
    }

    /** Splits one CSV line, honouring quoted fields such as "Priya, S" and doubled quotes. */
    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out;
    }

    private static List<UploadRow> fromTable(List<List<String>> table) {
        if (table.isEmpty()) {
            throw new IllegalArgumentException("The file is empty.");
        }
        List<String> header = table.get(0);
        int emailColumn = -1;
        int nameColumn = -1;
        int roleColumn = -1;
        for (int i = 0; i < header.size(); i++) {
            switch (header.get(i).strip().toLowerCase(Locale.ROOT)) {
                case "email" -> emailColumn = i;
                case "name" -> nameColumn = i;
                case "role" -> roleColumn = i;
                default -> {
                }
            }
        }
        if (emailColumn < 0) {
            throw new IllegalArgumentException("The first row needs a column called \"email\".");
        }
        List<UploadRow> rows = new ArrayList<>();
        for (int r = 1; r < table.size(); r++) {
            List<String> cells = table.get(r);
            String email = value(cells, emailColumn);
            String name = value(cells, nameColumn);
            String role = value(cells, roleColumn);
            if (email.isEmpty() && name.isEmpty() && role.isEmpty()) {
                continue;
            }
            rows.add(new UploadRow(r + 1, email, name, role));
        }
        return rows;
    }

    private static String value(List<String> cells, int column) {
        return column < 0 || column >= cells.size() ? "" : cells.get(column).strip();
    }
}
