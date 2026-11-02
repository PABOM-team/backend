package com.pabom.backend.careguideline.application.csv;

import com.pabom.backend.careguideline.presentation.response.ImportValidationIssue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class Utf8CsvParser {
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    public CsvTable parse(String fileKey, MultipartFile file, List<String> expectedHeaders,
            List<ImportValidationIssue> issues) {
        if (file == null || file.isEmpty()) {
            issues.add(issue(fileKey, null, null, "FILE_REQUIRED", "CSV 파일이 비어 있습니다."));
            return new CsvTable(expectedHeaders, List.of());
        }
        if (file.getSize() > MAX_BYTES) {
            issues.add(issue(fileKey, null, null, "FILE_TOO_LARGE", "CSV 파일은 2MB 이하여야 합니다."));
            return new CsvTable(expectedHeaders, List.of());
        }
        String text;
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(file.getBytes()));
            text = decoded.toString();
        } catch (CharacterCodingException e) {
            issues.add(issue(fileKey, null, null, "INVALID_ENCODING", "UTF-8 인코딩 CSV만 지원합니다."));
            return new CsvTable(expectedHeaders, List.of());
        } catch (IOException e) {
            issues.add(issue(fileKey, null, null, "FILE_READ_FAILED", "CSV 파일을 읽을 수 없습니다."));
            return new CsvTable(expectedHeaders, List.of());
        }
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> records;
        try {
            records = records(text);
        } catch (IllegalArgumentException e) {
            issues.add(issue(fileKey, null, null, "INVALID_CSV", e.getMessage()));
            return new CsvTable(expectedHeaders, List.of());
        }
        if (records.isEmpty()) {
            issues.add(issue(fileKey, 1, null, "HEADER_REQUIRED", "CSV 헤더가 없습니다."));
            return new CsvTable(expectedHeaders, List.of());
        }
        List<String> actualHeaders = records.getFirst();
        if (!actualHeaders.equals(expectedHeaders)) {
            issues.add(issue(fileKey, 1, null, "INVALID_HEADER",
                    "필수 헤더와 순서가 일치하지 않습니다. expected=" + expectedHeaders));
            return new CsvTable(actualHeaders, List.of());
        }
        List<CsvTable.Row> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> values = records.get(index);
            if (values.size() == 1 && values.getFirst().isBlank()) continue;
            int rowNumber = index + 1;
            if (values.size() != actualHeaders.size()) {
                issues.add(issue(fileKey, rowNumber, null, "INVALID_COLUMN_COUNT",
                        "컬럼 수가 헤더와 일치하지 않습니다."));
                continue;
            }
            Map<String, String> mapped = new LinkedHashMap<>();
            for (int column = 0; column < actualHeaders.size(); column++) {
                mapped.put(actualHeaders.get(column), values.get(column));
            }
            rows.add(new CsvTable.Row(rowNumber, mapped));
        }
        return new CsvTable(actualHeaders, rows);
    }

    private List<List<String>> records(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"'); i++;
                    } else quoted = false;
                } else field.append(c);
            } else if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == ',') {
                record.add(field.toString()); field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                record.add(field.toString()); field.setLength(0);
                records.add(record); record = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (quoted) throw new IllegalArgumentException("닫히지 않은 따옴표가 있습니다.");
        if (!field.isEmpty() || !record.isEmpty()) {
            record.add(field.toString()); records.add(record);
        }
        return records;
    }

    private ImportValidationIssue issue(String file, Integer row, String column, String code, String message) {
        return new ImportValidationIssue(file, row, column, code, message);
    }
}
