package com.example.demo;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@RestController
public class CsvController {

    @GetMapping("/download/csv")
    public ResponseEntity<byte[]> downloadCsv() {
        // Sample data
        String[] header = {"ID", "Name", "Email"};
        String[][] data = {
                {"1", "Damitha", "damitha@example.com"},
                {"2", "Dilshan", "dilshan@example.com"}
        };

        // Write CSV into memory
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {
            // Write header
            writer.println(String.join(",", header));

            // Write rows
            for (String[] row : data) {
                writer.println(String.join(",", row));
            }
        }

        byte[] csvBytes = outputStream.toByteArray();

        // Return response with file download
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }


    @GetMapping("/download/csv-stream")
    public ResponseEntity<InputStreamResource> downloadCsvStream() {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("ID,Name,Email\n");
        csvBuilder.append("1,Damitha,damitha@example.com\n");
        csvBuilder.append("2,Dilshan,dilshan@example.com\n");

        byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(inputStream));
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/download/transactions-csv")
    public void downloadTransactionsCsv(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");

        try (PrintWriter writer = response.getWriter()) {
            // BOM for Excel (optional, but prevents "error opening file")
            writer.write('\uFEFF');

            // Header
            String[] headers = {
                    "crID","crEntreeID","ledgerID","amount","narration","chequeNo","cc","entryType",
                    "jobType","jobRefNo","refCode","crEntryDate","oldentreeID","poID","aEntreeID",
                    "entryCode","entryNo","aEntryDate","currancy","exchangeRate","authorized","authoBy",
                    "fYear","sysdatetime_entry","sysdatetime_authorized","entryLocation","split",
                    "voucherType","name","sysdatetime","user","company","cancel"
            };
            writer.println(String.join(",", headers));

            // Stream DB results
            jdbcTemplate.query(
                    "SELECT cr.crID, cr.entreeID AS crEntreeID, cr.ledgerID, cr.amount, cr.narration, " +
                            "cr.chequeNo, cr.cc, cr.entryType, cr.jobType, cr.jobRefNo, cr.refCode, cr.entryDate AS crEntryDate, " +
                            "cr.oldentreeID, cr.poID, a.entreeID AS aEntreeID, a.entryCode, a.entryNo, a.entryDate AS aEntryDate, " +
                            "a.currancy, a.exchangeRate, a.authorized, a.authoBy, a.fYear, a.sysdatetime_entry, a.sysdatetime_authorized, " +
                            "a.entryLocation, a.split, a.voucherType, a.name, a.sysdatetime, a.`user`, a.company, a.cancel " +
                            "FROM acc_transaction_cr_2016 cr " +
                            "LEFT JOIN ofm_accounts_v2.acc_transaction_2016 a ON a.entreeID = cr.entreeID",
                    rs -> {
                        StringBuilder row = new StringBuilder();
                        for (int i = 1; i <= headers.length; i++) {
                            if (i > 1) row.append(",");
                            row.append(escapeCsv(rs.getString(i)));
                        }
                        writer.println(row.toString());
                    }
            );
        }
    }

    // Escape CSV fields properly
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\""); // escape quotes
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\""; // wrap in quotes
        }
        return escaped;
    }

}
