package com.shareway.application.port.out;
import java.util.List; import java.util.Map;
public interface ExportPort {
    byte[] toCsv(List<Map<String, Object>> data, String[] headers);
    byte[] toExcel(List<Map<String, Object>> data, String sheetName, String[] headers);
}
