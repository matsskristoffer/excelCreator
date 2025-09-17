package org.matsi.excelcreator;

import static org.matsi.excelcreator.ExcelUtilities.returnStringFromCell;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Getter
public class ExcelReader {

  List<XSSFSheet> sheets = new ArrayList<>();

  public ExcelReader() {

  }

  private final Logger logger = LogManager.getLogger();


  public List<XSSFSheet> getSheetsFromExcelByteArray(byte[] bytes) throws IllegalStateException {
    try (InputStream fis = new ByteArrayInputStream(bytes);
        XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
      int numberOfSheets = workbook.getNumberOfSheets();
      for (int i = 0; i < numberOfSheets; i++) {
        sheets.add(workbook.getSheetAt(i));
      }
      return sheets;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<Row> getRowsFromExcelByteArrayOnSheetNumber(byte[] bytes, int sheetNumber) throws IllegalStateException {
    List<Row> rows = new ArrayList<>();

    try (InputStream inputStream = new ByteArrayInputStream(bytes);
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
      XSSFSheet xssfSheet = workbook.getSheetAt(sheetNumber);
      rows.addAll(getRowsFromSheet(xssfSheet));
    } catch (IOException e) {
      logger.error(e.getMessage());
      throw new IllegalStateException(e);
    }
    return rows;
  }

  public List<String> getSheetNames(byte[] bytes) throws IllegalStateException {
    try (InputStream fis = new ByteArrayInputStream(bytes);
        XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
      return workbook.getAllNames().stream().map(XSSFName::getSheetName).toList();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public List<XSSFSheet> getSheets(byte[] bytes) throws IllegalStateException {
    return getSheetsFromExcelByteArray(bytes);
  }

  public List<XSSFSheet> getMatchingSheets(String matchingStringInSheet) throws IllegalStateException {
    return getMatchingSheets(null, matchingStringInSheet);
  }


  public List<XSSFSheet> getMatchingSheets(byte[] bytes, String matchingStringInSheet) throws IllegalStateException {

    if (sheets.isEmpty() && bytes != null) {
      getSheets(bytes);
    }
    // Will sort and re-save sheets based on a matching string on any row.
    return sheets.stream().filter(sheet -> {
      List<Row> rows = getRowsFromSheet(sheet);
      return !(rows.isEmpty()) && matchingSheet(rows, matchingStringInSheet);
    }).toList();
  }

  public static List<Row> getRowsFromSheet(XSSFSheet sheet) {
    List<Row> rows = new ArrayList<>();
    for (int i = 0; i <= sheet.getLastRowNum(); i++) {
      rows.add(sheet.getRow(i));
    }
    return rows;
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