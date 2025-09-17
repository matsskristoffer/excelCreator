package org.matsi.excelcreator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Getter
public class CreateExcelWithData {

  XSSFWorkbook xssfWorkbook;
  ByteArrayOutputStream byteArrayOutputStream;

  public <T> CreateExcelWithData(Map<String, List<T>> listOfData, ObjectMapper objectMapper) {

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        XSSFWorkbook workbook = new XSSFWorkbook()) {

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      XSSFFont font = workbook.createFont();
      font.setFontName("Arial");
      font.setFontHeightInPoints((short) 16);
      font.setBold(true);
      headerStyle.setFont(font);

      AddExcelDataFromObject addExcelDataFromObject = new AddExcelDataFromObject(objectMapper, headerStyle);

      // Loop through the mapper, each key represents one sheet.
      listOfData.forEach((key, value) -> addExcelDataFromObject.addDataToSheet(value, workbook.createSheet(key)));

      workbook.write(byteArrayOutputStream);

      this.byteArrayOutputStream = byteArrayOutputStream;
      xssfWorkbook = workbook;

    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
