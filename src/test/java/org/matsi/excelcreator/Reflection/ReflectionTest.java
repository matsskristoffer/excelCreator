package org.matsi.excelcreator.Reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReflectionTest {


  record Hello(List<String> hello) {

  }

  record Intermediate(String intermediateString) {

  }

  record HelloWithIntermediate(List<Intermediate> intermediateList) {

  }


  @Test
  void testGetObject() {

    Field[] fields = Hello.class.getDeclaredFields();

    fields[0].setAccessible(true);

    List<String> object = (List<String>) Reflection.getObject(fields[0], "hej, hej");
    assertNotNull(object);

    assertEquals(List.of("hej", "hej"), object);

    object = (List<String>) Reflection.getObject(fields[0], "hej");

    assertEquals(List.of("hej"), object);

    object = (List<String>) Reflection.getObject(fields[0], "[hej, hej]");

    assertEquals(List.of("hej", "hej"), object);

    object = (List<String>) Reflection.getObject(fields[0], "{hello = hej}");

    // Still a bit of weird value but shouldn't happen.
    assertEquals(List.of("{hello = hej}"), object);

    Field[] fieldIntermediate = HelloWithIntermediate.class.getDeclaredFields();

    fieldIntermediate[0].setAccessible(true);

    // This will map it to a specific object
    Object object1 = Reflection.getObject(fieldIntermediate[0], "{intermediateString = hej}");
    object = (List<String>) Reflection.getObject(fieldIntermediate[0], "{intermediateString = hej}");

    assertEquals(List.of(new Intermediate("hej")), object);

    String name = fieldIntermediate[0].getType().getName();
    assertEquals("java.util.List", name );

  }
}