package org.disK.excelcreator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;


public class GetFieldFromClass {

  private final Class<?> clazz;
  private List<String> skipFields = new ArrayList<>();
  @Getter
  List<Field> fields;

  /**
   * Will return a list of declared fields in snake case format.
   *
   * @param clazz generic class
   */
  public GetFieldFromClass(Class<?> clazz, List<String> skipFields) {
    this.clazz = clazz;
    if (skipFields != null && !skipFields.isEmpty()) {
      this.skipFields = skipFields;
    }

    fields = getDeclaredFieldsFromClass();

  }


  private List<Field> getDeclaredFieldsFromClass() {

    return Arrays.stream(clazz.getDeclaredFields())
        .filter(name -> skipFields.isEmpty() || skipFields.stream().anyMatch(s -> s.equals(name.getName())))
        .toList();
  }

}
