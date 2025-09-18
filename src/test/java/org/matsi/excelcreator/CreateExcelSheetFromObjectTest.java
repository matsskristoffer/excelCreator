package org.matsi.excelcreator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.Test;

public class CreateExcelSheetFromObjectTest {

  @Test
  public void checkObjectAgainstExcel() throws IOException {

    var sheets = new ExcelReader().getSheets(FileReader.readFromFile("TestObject.xlsx"));

    Map<String, List<TestObject>> mapOfTestObjectFromExcel = sheets.stream()
        .collect(Collectors.toMap(XSSFSheet::getSheetName,
                                  v -> new CreateDataObjectFromExcel().createListOfObjectsFromExcelSheet(TestObject.class, v)));

    Entry<String, List<TestObject>> testObjects = mapOfTestObjectFromExcel.entrySet().stream().toList().getFirst();

    TestObject testObject = new TestObject("test", 1, "other", List.of("1", "2", "3", "4"));

    assertEquals(testObjects.getValue().getFirst(), testObject);

  }

  @Test
  public void addDataToSheetTest() throws IOException {

    TestObject testObject = new TestObject("test", 1, "other", List.of("1", "2", "3", "4"));

    CreateExcelWithData hello = new CreateExcelWithData(Map.of("hello", List.of(testObject)),
                                                        new ObjectMapper());

    var sheets = new ExcelReader().getSheets(FileReader.readFromFile("TestObject.xlsx"));

    XSSFSheet hello1 = hello.getXssfWorkbook().getSheet("hello");

    XSSFSheet helloFromExcel = sheets.getFirst();

    assertEquals(ExcelUtilities.getRowOfDataAsStrings(helloFromExcel.getRow(1), 3),
                 ExcelUtilities.getRowOfDataAsStrings(hello1.getRow(1), 3));

    List<TestObject> mapOfTestObjectFromExcel =
        new CreateDataObjectFromExcel().createListOfObjectsFromExcelSheet(TestObject.class, hello.getXssfWorkbook().getSheetAt(0));

    assertEquals(mapOfTestObjectFromExcel.getFirst(), testObject);

  }
}