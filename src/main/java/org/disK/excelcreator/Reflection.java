package org.disK.excelcreator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Reflection {

  static Object getObject(Field field, String value) {
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
      } else if (String.class.isAssignableFrom(type)) {
        return value;
      } else {
        Object o1 = newInstanceOf(type);
        setFieldData(field, o1, value);
        return o1;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Enum<T>> T createEnumInstance(String name, Type type) {
    return Enum.valueOf((Class<T>) type, name);
  }

  public static <T> T newInstanceOf(Class<T> type) {
    T obj;
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      if (!constructor.canAccess(null)) {
        constructor.setAccessible(true);
      }
      obj = constructor.newInstance();
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot create a new instance of " + type.getName(), ex);
    }

    return obj;
  }

  public static void setFieldData(Field field, Object instance, Object o) {
    try {
      field.setAccessible(true);
      field.set(instance, o);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unexpected cast type {" + o + "} of field" + field.getName());
    }
  }

}
