package org.matsi.excelcreator;

import static org.matsi.excelcreator.Reflection.Reflection.getObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.matsi.excelcreator.ExcelUtilities.RowAndCellAddress;
import org.matsi.excelcreator.Reflection.GetFieldFromClass;
import org.matsi.excelcreator.Reflection.Reflection;

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

  /**
   * Will create a list of objects based on input Class, if annotation class are used it can extract the values too.
   *
   * @param clazz           Object class to map to in a parameterized type.
   * @param annotationClazz Annotation class if used
   * @param sheet           Excel sheet to read from
   * @param mapper          Jackson mapper if used
   * @param <T>             Parameterized type.
   * @return A list of object based on parameterized type.
   */
  public <T> List<T> createListOfObjectsFromExcelSheet(final Class<T> clazz,
      Class<? extends Annotation> annotationClazz,
      final XSSFSheet sheet,
      ObjectMapper mapper) {

    List<Field> fields = new GetFieldFromClass(clazz, annotationClazz).getFields();

    var rowsFromSheet = ExcelReader.getRowsFromSheet(sheet).stream().filter(Objects::nonNull).toList();

    Map<Field, HasAnnotationAndWithValue> mapOfFieldValueOrAnnotation = getFieldMatchOnNameOrAnnotation(fields, rowsFromSheet, annotationClazz);

    final Row indexRow = getRowAndCellAddressForIndexRow(rowsFromSheet, mapOfFieldValueOrAnnotation)
        .row();

    if (mapper == null) {
      return
          getObjectMapFromFieldNameOrAnnotation(indexRow, rowsFromSheet, mapOfFieldValueOrAnnotation)
              .stream()
              .map(mapObject -> getObject(clazz, fields, mapObject))
              .filter(row -> row.getClass().isAssignableFrom(clazz))
              .map(clazz::cast)
              .toList();

    } else {
      return mapToObject(getObjectMapFromFieldNameOrAnnotation(indexRow, rowsFromSheet, mapOfFieldValueOrAnnotation),
                         clazz,
                         mapper);
    }
  }

  /**
   * Will take a list of Fields and match which fields are sent in the Excel,
   * if an annotation value has been used it'll match against that too.
   *
   * @param fieldsFromClass A list of fields
   * @param rows            Rows from sheet
   * @param annotationClass the annotation class used.
   * @return a Map with Field as key and HasAnnotationAndWithValue object if it's annotated.
   */
  public Map<Field, HasAnnotationAndWithValue> getFieldMatchOnNameOrAnnotation(List<Field> fieldsFromClass,
      List<Row> rows,
      Class<? extends Annotation> annotationClass) {

    Map<Field, HasAnnotationAndWithValue> map = new HashMap<>();

    fieldsFromClass.forEach(
        field -> {
          Optional<RowAndCellAddress> rowAndCellAddressForString = ExcelUtilities.getRowAndCellAddressForString(rows, field.getName());
          if (rowAndCellAddressForString.isPresent()) {
            map.put(field, new HasAnnotationAndWithValue(false, ""));
          } else {
            if (annotationClass != null && field.isAnnotationPresent(annotationClass)
                && Arrays.stream(field.getAnnotationsByType(annotationClass)).findFirst().isPresent()) {
              map.put(field, new HasAnnotationAndWithValue(true, Reflection.getAnnotationValue(field, annotationClass)));
            }
          }
        });

    return map;


  }

  /**
   * Will get the index row based on a matcher based on field names or annotation values.
   *
   * @param rowsFromSheet Rows from sheet
   * @param mapOfFields   The map with Field as key and or annotation values.
   * @return A row and Cell address of the first match.
   */
  private RowAndCellAddress getRowAndCellAddressForIndexRow(List<Row> rowsFromSheet, Map<Field, HasAnnotationAndWithValue> mapOfFields) {
    return mapOfFields
        .entrySet()
        .stream()
        .map(s -> s.getValue().hasAnnotation() ? s.getValue().annotationValue() : s.getKey().getName())
        .map(string -> ExcelUtilities.getRowAndCellAddressForString(rowsFromSheet.stream().limit(10).toList(), string))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Couldn't find a field matching from excel"))
        .orElseThrow(() -> new IllegalStateException("No match on field, check spelling compared to excel"));
  }

  @Deprecated(since = "2025-09-22")
  private List<Field> getFieldsMatchingClassOrAnnotation(
      List<Field> fields,
      List<Row> rowsFromSheet) {
    return getFieldsMatchingClassOrAnnotation(fields, rowsFromSheet, null);
  }

  @Deprecated(since = "2025-09-22")
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


  /**
   * Generic to return a List of a parameterized type.
   *
   * @param list   List of simple Map where key is the Field name and a Object, can be a variable as such.
   * @param clazz  parameterized class
   * @param mapper jackson mapper to use
   * @return A list of objects on parameterized type.
   */
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

  /**
   * Will find correct map against index row for each dataRow,
   * if the index row has the annotation value it'll still treat it as a match, but it'll use the Field for when creating the object.
   *
   * @param indexRow Index row that have the match against the Fields in Data object or Annotation value.
   * @param rows     All the rows from the sheet
   * @param map      Map of Field as a key and extra metadata if it has an annotation value.
   * @return A list of Map<String, Object> to be returned.
   */
  private List<Map<String, Object>> getObjectMapFromFieldNameOrAnnotation(Row indexRow, List<Row> rows, Map<Field, HasAnnotationAndWithValue> map) {
    List<Map<String, Object>> listOfMapObjects = new ArrayList<>();

    Iterator<Row> iterator = rows.stream().iterator();
    while (iterator.hasNext()) {

      Map<String, Object> objectMap = new HashMap<>();
      Row next = iterator.next();

      if (next != null && !next.equals(indexRow)) {

        for (Cell cell : next) {
          CellAddress address = cell.getAddress();

          String fieldNameOrAnnotationFromExcel = indexRow.getCell(address.getColumn()).getStringCellValue();

          Optional<Field> fieldMatch = map.entrySet()
              .stream()
              .filter(fieldNameOrAnnotation ->
                          fieldNameOrAnnotation.getValue().hasAnnotation()
                              ? fieldNameOrAnnotationFromExcel.equalsIgnoreCase(
                              fieldNameOrAnnotation.getValue().annotationValue())
                              : fieldNameOrAnnotationFromExcel.equalsIgnoreCase(
                                  fieldNameOrAnnotation.getKey().getName()))
              .findFirst()
              .map(Entry::getKey);

          if (!ExcelUtilities.returnStringFromCell(cell).equals(" ")
              && fieldMatch.isPresent()) {

            fieldMatch.get().setAccessible(true);
            Object object = getObject(fieldMatch.get(), ExcelUtilities.returnStringFromCell(cell));
            objectMap.put(fieldMatch.get().getName(), object);
          }

        }

        listOfMapObjects.add(objectMap);
      }
    }

    return listOfMapObjects;

  }

  @Deprecated(since = "2025-09-22")
  private Map<String, Object> getObjectMapFromFieldNameOrAnnotation(List<Field> fields,
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
