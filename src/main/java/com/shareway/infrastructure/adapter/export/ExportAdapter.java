package com.shareway.infrastructure.adapter.export;

import com.opencsv.CSVWriter;
import com.shareway.application.port.out.ExportPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExportAdapter implements ExportPort {

    @Override
    public byte[] toCsv(List<Map<String, Object>> data, String[] headers) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(bos, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.writeNext(headers);
            for (Map<String, Object> row : data) {
                String[] values = new String[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    Object v = row.get(headers[i]);
                    values[i] = v != null ? v.toString() : "";
                }
                writer.writeNext(values);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("CSV export failed", e);
        }
    }

    @Override
    public byte[] toExcel(List<Map<String, Object>> data, String sheetName, String[] headers) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int r = 0; r < data.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Map<String, Object> rowData = data.get(r);
                for (int c = 0; c < headers.length; c++) {
                    Object val = rowData.get(headers[c]);
                    Cell cell = row.createCell(c);
                    if (val instanceof Number n) cell.setCellValue(n.doubleValue());
                    else cell.setCellValue(val != null ? val.toString() : "");
                }
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Excel export failed", e);
        }
    }
}
