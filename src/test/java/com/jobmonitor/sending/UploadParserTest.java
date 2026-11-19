package com.jobmonitor.sending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class UploadParserTest {

    @Test
    void readsCsvWithHeadersAndQuotedNames() throws IOException {
        String csv = "email,name,role\n"
                + "priya.s@example.com,\"Priya, S\",Java Developer\n"
                + "\n"
                + "rahul.k@example.com,,\n";
        List<UploadRow> rows = UploadParser.parse("list.csv",
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(rows).containsExactly(
                new UploadRow(2, "priya.s@example.com", "Priya, S", "Java Developer"),
                new UploadRow(4, "rahul.k@example.com", "", ""));
    }

    @Test
    void readsExcelWhateverTheColumnOrder() throws IOException {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Recruiters");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Company");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("Name");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Infosys");
            row.createCell(1).setCellValue("anil.m@infosys.com");
            row.createCell(2).setCellValue("Anil");
            workbook.write(out);
            file = out.toByteArray();
        }
        List<UploadRow> rows = UploadParser.parse("list.xlsx", new ByteArrayInputStream(file));
        assertThat(rows).containsExactly(new UploadRow(2, "anil.m@infosys.com", "Anil", ""));
    }

    @Test
    void needsAnEmailColumn() {
        assertThatThrownBy(() -> UploadParser.parse("list.csv",
                new ByteArrayInputStream("name\nPriya\n".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rejectsOtherFileTypes() {
        assertThatThrownBy(() -> UploadParser.parse("list.txt", new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitsQuotedFieldsWithDoubledQuotes() {
        assertThat(UploadParser.splitCsv("a,\"b \"\"c\"\"\",d")).containsExactly("a", "b \"c\"", "d");
    }
}
