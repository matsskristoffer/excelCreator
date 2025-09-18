package org.matsi.excelcreator;

import static org.matsi.excelcreator.ExcelUtilities.getRowOfDataAsStrings;
import static org.matsi.excelcreator.Reflection.Reflection.getObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.matsi.excelcreator.ExcelUtilities.RowAndCellAddress;
import org.matsi.excelcreator.Reflection.GetFieldFromClass;

public class CreateDataObjectFromExcel {

  public <T> List<T> createListOfObjectsFromExcelSheet(
      Class<T> clazz,
      final XSSFSheet sheet) {

    return createListOfObjectsFromExcelSheet(clazz,
                                             null,
                                             sheet,
                                             null);
  }

  public <T> List<T> createListOfObjectsFromExcelSheet(
      Class<T> clazz,
      Class<? extends Annotation> annotationClazz,
      final XSSFSheet sheet) {

    return createListOfObjectsFromExcelSheet(clazz,
                                             annotationClazz,
                                             sheet,
                                             null);
  }

  public <T> List<T> createListOfObjectsFromExcelSheet(
      Class<T> clazz,
      Class<? extends Annotation> annotationClazz,
      final XSSFSheet sheet,
      ObjectMapper objectMapper) {

    return createListOfObjectsFromExcelSheet(clazz,
                                             annotationClazz,
                                             sheet,
                                             new GetFieldFromClass(clazz, annotationClazz).getFields(),
                                             objectMapper);
  }


  public <T> List<T> createListOfObjectsFromExcelSheet(final Class<T> clazz,
      Class<? extends Annotation> annotationClazz,
      final XSSFSheet sheet,
      List<Field> fields,
      ObjectMapper mapper) {

    List<String> fieldNames = fields.stream().map(Field::getName).toList();

    var rowsFromSheet = ExcelReader.getRowsFromSheet(sheet).stream().filter(Objects::nonNull).toList();
    var rowsWithData = getRowsWithData(rowsFromSheet, fieldNames);

    List<Field> matchingFieldsInExcelWithoutAnnotation = getFieldsMatchingClassOrAnnotation(fields, rowsFromSheet);

    final RowAndCellAddress rowAndCellAddressForIndexData = getRowAndCellAddress(rowsFromSheet, matchingFieldsInExcelWithoutAnnotation, fieldNames);

    List<Field> matchingFieldsInExcelWithAnnotation = getFieldsMatchingClassOrAnnotation(fields, rowsFromSheet, annotationClazz);

    if (mapper == null) {

      return rowsWithData.stream()
          .map(row -> getRowOfDataAsStrings(row, rowAndCellAddressForIndexData.row().getLastCellNum()))
          .map(row -> getObjectMap(fields, row, matchingFieldsInExcelWithAnnotation))
          .map(row -> getObject(clazz, fields, row))
          .filter(row -> row.getClass().isAssignableFrom(clazz))
          .map(clazz::cast)
          .toList();

    } else {

      return mapToObject(rowsWithData.stream()
                             .map(row -> getRowOfDataAsStrings(row, rowAndCellAddressForIndexData.row().getLastCellNum()))
                             .map(row -> getObjectMap(fields, row, matchingFieldsInExcelWithAnnotation))
                             .toList(),
                         clazz,
                         mapper);
    }
  }

  private static RowAndCellAddress getRowAndCellAddress(List<Row> rowsFromSheet, List<Field> matchingFieldsInExcel, List<String> fieldNames) {
    return ExcelUtilities
        .getRowAndCellAddressForString(rowsFromSheet, matchingFieldsInExcel.stream()
            .filter(field -> fieldNames.contains(field.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Couldn't find a field matching from excel"))
            .getName())
        .orElseThrow(() -> new IllegalStateException("Missing last column for object from sheet"));
  }

  private static List<Row> getRowsWithData(List<Row> rowsFromSheet, List<String> fieldNames) {
    // Will sort out all rows that have a matching field
    // Will filter out rows that just have empty space.
    return rowsFromSheet.stream()
        .filter(
            row -> fieldNames.stream().noneMatch(fieldName -> ExcelUtilities.getRowOfDataAsStrings(row, row.getLastCellNum()).contains(fieldName)))
        // Will filter out rows that just have empty space.
        .filter(s -> ExcelUtilities.getCellValuesFromRow(s).stream().noneMatch(string -> string == null || string.equals(" ")))
        .toList();
  }

  private static List<Field> getFieldsMatchingClassOrAnnotation(
      List<Field> fields,
      List<Row> rowsFromSheet) {
    return getFieldsMatchingClassOrAnnotation(fields, rowsFromSheet, null);
  }

  private static List<Field> getFieldsMatchingClassOrAnnotation(
      List<Field> fields,
      List<Row> rowsFromSheet,
      Class<? extends Annotation> annotationClass) {
    return fields.stream().filter(
            field -> {
              Optional<RowAndCellAddress> rowAndCellAddressForString = ExcelUtilities.getRowAndCellAddressForString(rowsFromSheet, field.getName());
              if (rowAndCellAddressForString.isPresent()) {
                return true;
              } else {
                return annotationClass != null && field.isAnnotationPresent(annotationClass);
              }
            })
        .toList();
  }

  private <T> List<T> mapToObject(List<Map<String, Object>> list, Class<T> clazz, ObjectMapper mapper) {

    if (mapper == null) {
      mapper = new ObjectMapper();
    }

    List<T> arrayList = new ArrayList<>();
    for (Map<String, Object> map : list) {

      String s;
      try {
        s = mapper.writeValueAsString(map);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      try {
        Object o = mapper.readValue(s, clazz);
        if (o.getClass().isAssignableFrom(clazz)) {
          arrayList.add(clazz.cast(o));
        }
      } catch (JsonProcessingException mismatchedInputException) {
        throw new IllegalArgumentException("Couldn't loop through this object:\n" + s + "\nException: " + mismatchedInputException);
      } catch (ClassCastException classCastException) {
        throw new IllegalArgumentException("Couldn't cast this object:\n" + s + "\nException: " + classCastException);
      }
    }

    return arrayList;

  }

  private Map<String, Object> getObjectMap(List<Field> fields,
      List<String> firstRowObject,
      List<Field> fieldNamesFromExcel) {
    Map<String, Object> objectMap = new HashMap<>();
    // Will try to map the value against the object values
    int i = 0;
    for (Field fieldFromExcel : fieldNamesFromExcel.stream().filter(Objects::nonNull).toList()) {
      String valueFromExcel = " ";
      if (i < firstRowObject.size() - 1) {
        valueFromExcel = firstRowObject.get(i);
      }

      Optional<Field> fieldFromClass = fields.stream().filter(s -> s.getName().equals(fieldFromExcel.getName())).findFirst();

      if (fieldFromClass.isPresent()
          && !Objects.equals(valueFromExcel, " ")) {

        fieldFromClass.get().setAccessible(true);
        Object object = getObject(fieldFromClass.get(), valueFromExcel);
        objectMap.put(fieldFromClass.get().getName(), object);
      } else {
        objectMap.put(fieldFromExcel.getName(), null);
      }
      i++;
    }
    return objectMap;
  }

}
