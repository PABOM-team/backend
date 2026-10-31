package com.pabom.backend.careguideline.application.csv;

import java.util.List;
import java.util.Map;

public record CsvTable(List<String> headers, List<Row> rows) {
    public record Row(int number, Map<String, String> values) {
        public String get(String header) { return values.get(header); }
    }
}
