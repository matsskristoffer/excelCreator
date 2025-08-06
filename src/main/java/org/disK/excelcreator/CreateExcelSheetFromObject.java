package org.disK.excelcreator;

import static org.disK.excelcreator.GetCellAddress.findColumnPosition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;

public class CreateExcelSheetFromObject {

  private final ObjectMapper mapper;
  private final CellStyle headerStyle;
  private final Class<?> clazz;
  private final List<String> fields;

  public CreateExcelSheetFromObject(ObjectMapper mapper, CellStyle headerStyle, Class<?> clazz) {
    this.mapper = mapper;
    this.headerStyle = headerStyle;
    this.clazz = clazz;
    fields = new GetFieldFromClass(this.clazz, null)
        .getFields()
        .stream()
        .map(Field::getName)
        .toList();
  }

  public void addDataToSheet(List<?> entries, Sheet sheet) throws IllegalStateException {

    if (entries.isEmpty()) {
      throw new IllegalStateException("List sent in was empty!");
    }

    Row indexRow = sheet.createRow(0);

    createIndexColumns(sheet, fields, indexRow);

    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
    };

    // Use iterator instead of indexOf which doesn't work that well
    int i = indexRow.getRowNum() + 1;
    for (Object entry : entries) {
      try {
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry);
        Map<String, Object> hashmapWithData = mapper.readValue(json, typeRef);
        if (hashmapWithData != null) {
          Row dataRow = sheet.createRow(i);
          writeRowWithObjectData(hashmapWithData, indexRow, fields, dataRow);
          i++;
        }
      } catch (JsonProcessingException e) {
        throw new IllegalStateException(e);
      }

    }
    if (sheet.getLastRowNum() < entries.size()) {
      throw new IllegalStateException("Couldn't map all the data!!");
    }
  }

  private void writeRowWithObjectData(Map<String, Object> hashmapWithData, Row indexRow, List<String> fields, Row dataRow) {
    for (Entry<String, Object> data : hashmapWithData.entrySet()) {

      CellAddress columnPosition = findColumnPosition(data.getKey(), indexRow, fields.size());
      if (columnPosition != null) {
        String dataValue = data.getValue().toString();
        if (dataValue.length() > 32767) {
          // If the value is too big for one cell, we'll just omit the rest of the value.
          String[] splitString = dataValue.split("(?<=\\G.{" + 32767 + "})");
          dataRow.createCell(columnPosition.getColumn()).setCellValue(splitString[0]);
        } else {
          // Ugly empty array that we can skip and just replace with an empty String
          if (dataValue.equals("[]")) {
            dataRow.createCell(columnPosition.getColumn()).setCellValue(" ");
          } else {
            dataRow.createCell(columnPosition.getColumn()).setCellValue(dataValue);
          }
        }
      }
    }
  }

  private void createIndexColumns(Sheet sheet, List<String> fields, Row indexRow) {
    for (String fieldName : fields) {
      Cell cell = indexRow.createCell(fields.indexOf(fieldName));
      cell.setCellStyle(headerStyle);
      cell.setCellValue(fieldName);
      sheet.autoSizeColumn(fields.indexOf(fieldName));
    }
  }

}
