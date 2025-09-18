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
                                             sheet,
                                             new GetFieldFromClass(clazz, annotationClazz).getFields(),
                                             objectMapper);
  }


  public <T> List<T> createListOfObjectsFromExcelSheet(final Class<T> clazz, final XSSFSheet sheet, List<Field> fields, ObjectMapper mapper) {

    var rowsFromSheet = ExcelReader.getRowsFromSheet(sheet).stream().filter(Objects::nonNull).toList();

    var rowsWithData = rowsFromSheet.stream()
        .skip(1)
        .toList();

    List<Field> matchingFieldsInExcel = fields.stream().filter(
            field -> ExcelUtilities.getRowAndCellAddressForString(rowsFromSheet, field.getName()).isPresent())
        .toList();

    final RowAndCellAddress rowAndCellAddressForIndexData = ExcelUtilities
        .getRowAndCellAddressForString(rowsFromSheet, matchingFieldsInExcel.get(matchingFieldsInExcel.size() - 1).getName())
        .orElseThrow(() -> new IllegalArgumentException("Missing last column for object from sheet"));

    short lastCellNum = rowAndCellAddressForIndexData.row().getLastCellNum();

    List<String> fieldNamesFromExcel = matchingFieldsInExcel.stream().map(Field::getName).toList();

    if (mapper == null) {

      return rowsWithData.stream()
          .map(row -> getRowOfDataAsStrings(row, lastCellNum))
          .map(row -> getObjectMap(fields, row, fieldNamesFromExcel))
          .map(row -> getObject(clazz, fields, row))
          .filter(row -> row.getClass().isAssignableFrom(clazz))
          .map(clazz::cast)
          .toList();

    } else {

      return mapToObject(rowsWithData.stream()
                             .map(row -> getRowOfDataAsStrings(row, lastCellNum))
                             .map(row -> getObjectMap(fields, row, fieldNamesFromExcel))
                             .toList(),
                         clazz,
                         mapper);
    }
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
      List<String> fieldNamesFromExcel) {
    Map<String, Object> objectMap = new HashMap<>();
    // Will try to map the value against the object values
    int i = 0;
    for (String fieldFromExcel : fieldNamesFromExcel.stream().filter(Objects::nonNull).toList()) {
      String valueFromExcel = " ";
      if (i < firstRowObject.size() - 1) {
        valueFromExcel = firstRowObject.get(i);
      }

      Optional<Field> fieldFromClass = fields.stream().filter(s -> s.getName().equals(fieldFromExcel)).findFirst();

      if (fieldFromClass.isPresent()
          && !Objects.equals(valueFromExcel, " ")) {

        fieldFromClass.get().setAccessible(true);
        Object object = getObject(fieldFromClass.get(), valueFromExcel);
        objectMap.put(fieldFromClass.get().getName(), object);
      } else {
        objectMap.put(fieldFromExcel, null);
      }
      i++;
    }
    return objectMap;
  }

}
