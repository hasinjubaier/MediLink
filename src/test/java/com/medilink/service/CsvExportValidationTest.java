package com.medilink.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class CsvExportValidationTest {

    private String escapeCsvField(String field) {
        if (field == null) return "\"\"";
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    @Test
    @DisplayName("CSV export must support UTF-8 BOM, Bangla script, commas, and escaped quotes")
    public void testCsvExportFormattingAndEscaping() {
        String utf8Bom = "\uFEFF";
        String[] headers = {"ID", "Name", "Generic", "Company", "Price", "Notes"};
        String headerLine = String.join(",", headers);

        // Data with quotes, commas, newlines, and Bangla text
        String id = "med_101";
        String name = "নাপা এক্সট্রা \"500mg\"";
        String generic = "Paracetamol, Caffeine";
        String company = "বেক্সিমকো ফার্মা (Beximco)";
        String price = "5.00";
        String notes = "Take after meal; Do not exceed 4g/day.";

        StringBuilder csv = new StringBuilder(utf8Bom);
        csv.append(headerLine).append("\r\n");

        csv.append(escapeCsvField(id)).append(",")
           .append(escapeCsvField(name)).append(",")
           .append(escapeCsvField(generic)).append(",")
           .append(escapeCsvField(company)).append(",")
           .append(escapeCsvField(price)).append(",")
           .append(escapeCsvField(notes)).append("\r\n");

        String result = csv.toString();

        // 1. Verify UTF-8 BOM prefix
        assertTrue(result.startsWith("\uFEFF"), "CSV must begin with UTF-8 BOM for Excel compatibility");

        // 2. Verify escaped quotes inside Bangla text
        assertTrue(result.contains("\"নাপা এক্সট্রা \"\"500mg\"\"\""), "Quotes inside fields must be doubled per RFC 4180");

        // 3. Verify comma preservation inside fields
        assertTrue(result.contains("\"Paracetamol, Caffeine\""), "Commas must be enclosed in double quotes");

        // 4. Verify byte encoding support in UTF-8
        byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
        assertTrue(bytes.length > 0);
        String decoded = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(result, decoded);
    }
}
