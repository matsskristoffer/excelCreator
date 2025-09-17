package org.matsi.excelcreator.Reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;


public class GetFieldFromClass {

  private final Class<?> clazz;
  private List<String> skipFields = new ArrayList<>();
  @Getter
  List<Field> fields;

  public GetFieldFromClass(Class<?> clazz) {
    this(clazz, null);
  }

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

  public List<Field> getStringsFromFieldNameOrAnnotationClass(Class<? extends Annotation> annotationClass) {
    List<Field> list = getDeclaredFieldsFromClass().stream().toList();
    List<Annotation[]> list1 = list.stream().map(AccessibleObject::getAnnotations).toList();

    // Use annotations instead
    if (annotationClass != null
        && !list1.isEmpty()
        && list1.stream().anyMatch(annotations -> Arrays.stream(annotations).anyMatch(annotation -> annotation.annotationType()
        .isAssignableFrom(annotationClass)))) {

      // If we're missing a JsonProperty from the class object we'll just ignore that part.
      return list.stream()
          .filter(field -> field.isAnnotationPresent(annotationClass))
          .filter(field -> !list.contains(field))
          .collect(Collectors.toCollection(() -> new ArrayList<>(list)));
    }
    return list;
  }

}
