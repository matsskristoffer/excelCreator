package org.matsi.excelcreator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFCreationHelper;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsi.excelcreator.Reflection.GetFieldFromClass;
import org.matsi.excelcreator.Reflection.Reflection;

public class CreateExcelSheetFromObject {

  private final ObjectMapper mapper;
  private final CellStyle headerStyle;
  private final ExcelUtilities excelUtilities = new ExcelUtilities();
  private XSSFWorkbook workbook = null;
  private Class<?> clazz;
  private Class<? extends Annotation> annotationClass = null;
  private List<Field> fields;

  public CreateExcelSheetFromObject(ObjectMapper mapper, CellStyle headerStyle) {
    this.mapper = mapper;
    this.headerStyle = headerStyle;
  }

  public CreateExcelSheetFromObject(ObjectMapper mapper, CellStyle headerStyle, Class<?> clazz) {
    this.mapper = mapper;
    this.headerStyle = headerStyle;
    this.clazz = clazz;
    this.fields = new GetFieldFromClass(this.clazz, annotationClass).getFields();
  }

  public CreateExcelSheetFromObject(ObjectMapper mapper, CellStyle headerStyle, Class<?> clazz, Class<? extends Annotation> annotationClass) {
    this.mapper = mapper;
    this.headerStyle = headerStyle;
    this.clazz = clazz;
    this.annotationClass = annotationClass;
    this.fields = new GetFieldFromClass(this.clazz, this.annotationClass).getFields();
  }

  public CreateExcelSheetFromObject(ObjectMapper mapper,
      CellStyle headerStyle,
      Class<?> clazz,
      Class<? extends Annotation> annotationClass,
      XSSFWorkbook workbook) {
    this.mapper = mapper;
    this.headerStyle = headerStyle;
    this.clazz = clazz;
    this.annotationClass = annotationClass;
    this.workbook = workbook;
    this.fields = new GetFieldFromClass(this.clazz, this.annotationClass).getFields();
  }

  public void addDataToSheet(List<?> entries, Sheet sheet) throws IllegalStateException {

    if (entries.isEmpty()) {
      throw new IllegalStateException("List sent in was empty!");
    }

    if (clazz == null && fields.isEmpty()) {
      this.clazz = entries.getFirst().getClass();
      this.fields = new GetFieldFromClass(clazz, annotationClass).getFields();
    }

    List<String> fieldNames = fields.stream().map(Field::getName).toList();

    Row indexRow = sheet.createRow(0);

    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
    };

    setOriginalHeaderValues(sheet, headerStyle, fields, indexRow);

    setDataForEachRow(entries, sheet, indexRow, typeRef, fields);

    setHeaderValuesBasedOnAnnotation(sheet, fields, indexRow);

  }

  private void setOriginalHeaderValues(Sheet sheet,
      CellStyle headerStyle,
      List<Field> fields,
      Row indexRow) {
    for (Field field : fields) {
      Cell cell = indexRow.createCell(fields.indexOf(field));
      cell.setCellStyle(headerStyle);
      field.setAccessible(true);

      if (annotationClass != null && workbook != null && field.isAnnotationPresent(annotationClass)) {
        Optional<? extends Annotation> annotationsByType = Arrays.stream(field.getAnnotationsByType(annotationClass)).findFirst();

        if (annotationsByType.isPresent()) {

          XSSFClientAnchor anchor = new XSSFClientAnchor(100,
                                                         100,
                                                         100,
                                                         100,
                                                         cell.getColumnIndex(),
                                                         cell.getRowIndex(),
                                                         cell.getColumnIndex() + 2,
                                                         cell.getRowIndex() + 5);
          Drawing<?> drawing = sheet.createDrawingPatriarch();
          Comment comment = drawing.createCellComment(anchor);

          RichTextString commentRichTextString = workbook.getCreationHelper().createRichTextString(
              Reflection.getAnnotationValue(field, annotationClass)
                  .orElse(field.getName()));

          comment.setString(commentRichTextString);

          cell.setCellComment(comment);

        }
      }
      cell.setCellValue(field.getName());
    }
  }

  private void setDataForEachRow(List<?> entries,
      Sheet sheet,
      Row indexRow,
      TypeReference<HashMap<String, Object>> typeRef,
      List<Field> fields) {
    if (!entries.isEmpty()) {
      // Use iterator instead of indexOf which doesn't work that well
      int i = indexRow.getRowNum() + 1;
      for (Object entry : entries) {
        try {

          String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entry);

          Map<String, Object> hashmapWithData = mapper.readValue(json, typeRef);

          if (hashmapWithData != null) {
            Row dataRow = sheet.createRow(i);

            for (Entry<String, Object> data : hashmapWithData.entrySet()) {

              CellAddress columnPosition = excelUtilities.findColumnPosition(data.getKey(), indexRow, fields.size());
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
                    Cell cell = dataRow.createCell(columnPosition.getColumn());
                    cell.setCellValue(dataValue);
                    if (dataValue.contains("http")) {
                      XSSFHyperlink hyperlink = new XSSFCreationHelper(workbook).createHyperlink(HyperlinkType.URL);
                      hyperlink.setAddress(dataValue);
                      cell.setHyperlink(hyperlink);
                    }
                  }
                }
              }
            }
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
  }

  private void setHeaderValuesBasedOnAnnotation(Sheet sheet, List<Field> fields, Row indexRow) {
    // Will do a quick autoSize for each column.
    for (Field field : fields) {
      if (annotationClass != null
          && field.isAnnotationPresent(annotationClass)) {
        // Will change the value of the header cell after all data is written, if the annotation with 'name' exists.
        Cell cell = indexRow.getCell(fields.indexOf(field));
        Optional<String> annotationsByType = Reflection.getAnnotationValue(field, annotationClass);
        // Will try to fetch name value, otherwise it'll use the field value.
        cell.setCellValue(annotationsByType.orElse(field.getName()));
      }
      sheet.autoSizeColumn(fields.indexOf(field));
    }
  }


  private void writeRowWithObjectData(Map<String, Object> hashmapWithData, Row indexRow, List<String> fields, Row dataRow) {
    for (Entry<String, Object> data : hashmapWithData.entrySet()) {

      CellAddress columnPosition = excelUtilities.findColumnPosition(data.getKey(), indexRow, fields.size());
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

//  private void createIndexColumns(Sheet sheet, List<String> fields, Row indexRow) {
//    for (String fieldName : fields) {
//      Cell cell = indexRow.createCell(fields.indexOf(fieldName));
//      cell.setCellStyle(headerStyle);
//      cell.setCellValue(fieldName);
//      sheet.autoSizeColumn(fields.indexOf(fieldName));
//    }
//  }

}
