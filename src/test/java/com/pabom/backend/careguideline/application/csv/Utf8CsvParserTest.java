package com.pabom.backend.careguideline.application.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.careguideline.presentation.response.ImportValidationIssue;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class Utf8CsvParserTest {
    private final Utf8CsvParser parser = new Utf8CsvParser();

    @Test
    void acceptsUtf8BomAndQuotedCommasWithoutChangingValues() {
        byte[] content = concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                "code,text\r\nA,\"VCA, RSPCA\"\r\n".getBytes(StandardCharsets.UTF_8));
        List<ImportValidationIssue> issues = new ArrayList<>();

        CsvTable table = parser.parse("test", new MockMultipartFile("test", "test.csv", "text/csv", content),
                List.of("code", "text"), issues);

        assertThat(issues).isEmpty();
        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().getFirst().get("text")).isEqualTo("VCA, RSPCA");
    }

    @Test
    void rejectsMalformedUtf8WithFileLevelError() {
        List<ImportValidationIssue> issues = new ArrayList<>();

        parser.parse("test", new MockMultipartFile("test", "test.csv", "text/csv",
                new byte[]{(byte) 0xC3, (byte) 0x28}), List.of("code"), issues);

        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.file()).isEqualTo("test");
            assertThat(issue.row()).isNull();
            assertThat(issue.code()).isEqualTo("INVALID_ENCODING");
        });
    }

    private byte[] concat(byte[] left, byte[] right) {
        byte[] value = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, value, left.length, right.length);
        return value;
    }
}
