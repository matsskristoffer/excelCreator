package org.matsi.excelcreator.Reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public class Reflection {

  public static Object getObject(Field field, String value) {
    Class<?> type = field.getType();
    return getObject(type, field, value);
  }

  static Object getObject(Class<?> type, Field field, String value) {

    if (value != null) {
      if (int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)) {
        return Integer.parseInt(value);
        // Just hope it's an offsetDate time
      } else if (OffsetDateTime.class.isAssignableFrom(type)) {
        // probably a date.
        try {
          return OffsetDateTime.parse(value);
        } catch (RuntimeException e) {
          return value;
        }
      } else if (Enum.class.isAssignableFrom(type)) {
        return createEnumInstance(value, type);
      } else if (LocalDate.class.isAssignableFrom(type)) {
        return LocalDate.parse(value);
      } else if (List.class.isAssignableFrom(type)) {
        return getObjectFromListClass(field, value);
      } else if (String.class.isAssignableFrom(type)) {
        return value;
      } else if (boolean.class.isAssignableFrom(type)) {
        return value;
      } else {
        return newInstanceOf(type, field, value);
      }
    }
    return null;
  }

  public static Optional<String> getAnnotationValue(Field field, Class<? extends Annotation> annotationClass) {
    return Arrays.stream(field.getAnnotationsByType(annotationClass))
        .findFirst()
        .map(annotation -> StringUtils.substringBetween(annotation.toString(), "{", "}")
                 .replace("\"", ""));
  }

  public static @Nullable Object getObjectFromListClass(Field field, String value) {
    // Only supports one level of lists for now.
    if (field != null) {
      ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
      Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];

      if (stringListClass.isAssignableFrom(String.class)) {

        return Arrays.stream(value.split(","))
            .map(string -> getObject(stringListClass, null, string
                .replace("[", "")
                .replace("]", "")
                .trim()))
            .toList();
      } else if (value.contains("=")
          && value.contains("{")
          && value.contains("}")) {

        Field[] declaredFields = stringListClass.getDeclaredFields();

        List<Field> list = Arrays.stream(declaredFields).filter(s -> value.contains(s.getName())).toList();

        List<Object> objectList = new ArrayList<>();

        String[] objectValues = value.split("=");

        // Only supports one item per list for now.. Should find out a better iteration over different objects in the list.
        for (Field field1 : list) {
          for (String objectValue : objectValues) {
            String trimmedValue = objectValue
                .replace("[", "")
                .replace("]", "")
                .replace(",", "")
                .replace("{", "")
                .replace("}", "")
                .replace(field1.getName(), "")
                .trim();
            if (!trimmedValue.isEmpty()) {
              field1.setAccessible(true);
              Object object = getObject(stringListClass, field1, trimmedValue);
              objectList.add(object);
            }
          }

        }
        return objectList;

      }
    } else {
      return value;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> T createEnumInstance(String name, Type type) {
    return Enum.valueOf((Class<T>) type, name);
  }

  public static <T> T getObject(Class<T> clazz,
      List<Field> fields,
      Map<String, Object> objectMap) {

    Object object = newInstanceOf(clazz);

    for (Field field : fields) {
      setFieldData(field, object, objectMap.get(field.getName()));
    }

    try {
      return clazz.cast(object);
    } catch (ClassCastException classCastException) {
      throw new IllegalArgumentException(classCastException);
    }

  }

  public static <T> T newInstanceOf(Class<T> type) {
    return newInstanceOf(type, null, null);
  }

  public static <T> T newInstanceOf(Class<T> type, Field field) {
    return newInstanceOf(type, field, null);
  }

  public static <T> T newInstanceOf(Class<T> type, Field field, String value) {
    T obj;
    try {
      Constructor<?>[] constructors = type.getDeclaredConstructors();

      if (Arrays.stream(constructors).toList().isEmpty()) {
        throw new IllegalStateException("Couldn't find any constructors, for class: " + type);
      }
      // Couldn't create instance, maybe use value instead.
      Optional<Constructor<?>> constructorWithOneArgument = Arrays.stream(constructors).filter(s -> s.getParameterCount() == 1).findFirst();
      if (value != null && constructorWithOneArgument.isPresent()) {

        if (!constructorWithOneArgument.get().canAccess(null)) {
          constructorWithOneArgument.get().setAccessible(true);
        }

        obj = type.cast(constructorWithOneArgument.get().newInstance(value));

      } else if (value != null) {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        obj = constructor.newInstance();
        setFieldData(field, obj, value);
      } else if (Arrays.stream(constructors).anyMatch(s -> s.getParameterCount() == 0)) {
        Constructor<T> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        obj = constructor.newInstance();
      } else {
        throw new IllegalStateException("Couldn't find a constructor with a single argument or empty argument");
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot create a new instance of " + type.getName() + "\n" + ex.getMessage(), ex);
    }

    return obj;
  }

  public static void setFieldData(Field field, Object instance, Object o) {
    try {
      if (o != null) {
        field.setAccessible(true);
        field.set(instance, o);
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unexpected cast type {" + o + "} of field" + field.getName());
    }
  }

}