package org.disK.excelcreator;

import static org.disK.excelcreator.ExcelUtilities.returnStringFromCell;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReader {


  public static List<Row> getRowsFromSheet(XSSFSheet sheet) {
    List<Row> rows = new ArrayList<>();
    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
      rows.add(sheet.getRow(i));
    }
    return rows;
  }

  public static List<XSSFSheet> getSheets(byte[] bytes) throws IOException {
    return getMatchingSheets(bytes, null);
  }

  public static List<XSSFSheet> getMatchingSheets(byte[] bytes, String matchingStringInSheet) throws IOException {

    List<XSSFSheet> sheetList = new ArrayList<>();
    InputStream inputStream = new ByteArrayInputStream(bytes);
    XSSFWorkbook workbook = new XSSFWorkbook(inputStream);

    int numberOfSheets = workbook.getNumberOfSheets();
    XSSFSheet sheet;
    for (int i = 0; i < numberOfSheets; i++) {
      sheet = workbook.getSheetAt(i);

      List<Row> rows = getRowsFromSheet(sheet);

      if (matchingStringInSheet == null || matchingSheet(rows, matchingStringInSheet) && (!rows.isEmpty())) {
        sheetList.add(sheet);
      }
    }
    return sheetList;
  }

  private static boolean matchingSheet(List<Row> rows, String matchingString) {

    Row firstRow = null;
    CellAddress cellAddress = null;
    Iterator<Row> rowIterator = rows.iterator();
    while (rowIterator.hasNext() && cellAddress == null) {
      Row row = rowIterator.next();
      while (row == null) {
        row = rowIterator.next();
      }
      Iterator<Cell> cellIterator = row.cellIterator();
      while (cellIterator.hasNext()) {
        Cell cell = cellIterator.next();
        if (returnStringFromCell(cell).toLowerCase().contains(matchingString.toLowerCase())) {
          // koncept/produkt was missing, so we match for this one, but we'll jump up one level to keep the same logic.
          // check for index out of bounds
          cellAddress = new CellAddress(cell.getAddress().getRow(), cell.getColumnIndex());
          firstRow = row;
          break;
        }
      }
    }

    return firstRow != null;
  }


}
