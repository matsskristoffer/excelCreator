package org.matsi.excelcreator;

import static org.matsi.excelcreator.ExcelUtilities.getRowOfDataAsStrings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.matsi.excelcreator.ExcelUtilities.RowAndCellAddress;
import org.matsi.excelcreator.Reflection.GetFieldFromClass;
import org.matsi.excelcreator.Reflection.Reflection;

public class CreateDataObjectFromExcel {


  public <T> List<T> createListOfObjectsFromExcelSheet(final Class<T> clazz, final XSSFSheet sheet) {

    return createListOfObjectsFromExcelSheet(clazz,
                                             sheet,
                                             new GetFieldFromClass(clazz, null).getFields(),
                                             null);

  }

  public <T> List<T> createListOfObjectsFromExcelSheet(final Class<T> clazz, final XSSFSheet sheet, List<Field> fields) {

    return createListOfObjectsFromExcelSheet(clazz,
                                             sheet,
                                             fields,
                                             null);

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

  private static <T> List<T> mapToObject(List<Map<String, Object>> list, Class<T> clazz, ObjectMapper mapper) {

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

  private <T> T getObject(Class<T> clazz,
      List<Field> fields,
      Map<String, Object> objectMap) {

    Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();

    // Will check for empty constructor which is required in order to create a new Instance.
    if (Arrays.stream(declaredConstructors).noneMatch(s -> Arrays.stream(s.getParameterTypes()).toList().isEmpty())) {
      throw new IllegalArgumentException("Cannot find empty constructor for class: " + clazz.getName());
    } else {

      Object object = Reflection.newInstanceOf(clazz);

      for (Field field : fields) {
        Reflection.setFieldData(field, object, objectMap.get(field.getName()));
      }

      try {
        return clazz.cast(object);
      } catch (ClassCastException classCastException) {
        throw new IllegalArgumentException(classCastException);
      }
    }

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
        Object object = Reflection.getObject(fieldFromClass.get(), valueFromExcel);
        objectMap.put(fieldFromClass.get().getName(), object);
      } else {
        objectMap.put(fieldFromExcel, null);
      }
      i++;
    }
    return objectMap;
  }

}
