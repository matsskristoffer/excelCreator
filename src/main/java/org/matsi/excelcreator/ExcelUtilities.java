package org.matsi.excelcreator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.matsi.excelcreator.Reflection.GetFieldFromClass;

public class ExcelUtilities {

  protected ExcelUtilities() {
  }

  public List<String> getColumnOfData(List<Row> rows, CellAddress cellAddress, int jumpNumberOfColumns) {

    List<Row> forwardJumpedRows = rows.stream().skip(cellAddress.getRow()).toList();
    Iterator<Row> rowIterator = forwardJumpedRows.iterator();

    List<String> data = new ArrayList<>();
    while (rowIterator.hasNext()) {
      Row row = rowIterator.next();
      if (row != null && row.getCell(cellAddress.getColumn() + jumpNumberOfColumns) != null) {
        Cell dataCell = row.getCell(cellAddress.getColumn() + jumpNumberOfColumns);
        String s = returnStringFromCell(dataCell);
        data.add(s);
      } else {
        data.add("");
      }
    }

    return data;

  }

  public boolean cellHasANonEmptyValue(Cell cell) {
    return cell != null && !returnStringFromCell(cell).replace(" ", "").isEmpty();
  }

  public boolean isMappableToClass(List<XSSFSheet> xssfSheets, Class<?> clazz) {
    return isMappableToClass(xssfSheets, clazz, null);
  }

  // Could probably use equals to a predefined object, but this is quicker
  public boolean isMappableToClass(List<XSSFSheet> xssfSheets, Class<?> clazz, Class<? extends Annotation> annotationClass) {
    boolean match = false;

    List<String> list = new GetFieldFromClass(clazz).getStringsFromFieldNameOrAnnotationClass(annotationClass).stream().map(Field::getName).toList();

    for (XSSFSheet sheet : xssfSheets) {
      List<Row> rows = ExcelReader.getRowsFromSheet(sheet).stream().filter(Objects::nonNull)
          .filter(s -> s.getLastCellNum() > 0)
          .limit(15)
          .toList();
      if (rows
          .stream()
          .anyMatch(s -> getRowOfDataAsStrings(s, list.size() - 1)
              .stream()
              .anyMatch(k -> list.stream().anyMatch(p -> p.equalsIgnoreCase(k))))) {
        match = true;
        break;
      }

    }

    return match;

  }

  public List<XSSFSheet> getMatchingSheetsFromClass(List<XSSFSheet> xssfSheets, Class<?> clazz) {
    return getMatchingSheetsFromClass(xssfSheets, clazz, null);
  }

  public List<XSSFSheet> getMatchingSheetsFromClass(List<XSSFSheet> xssfSheets, Class<?> clazz, Class<? extends Annotation> annotationClass) {
    List<String> list = new GetFieldFromClass(clazz).getStringsFromFieldNameOrAnnotationClass(annotationClass).stream().map(Field::getName).toList();

    return xssfSheets.stream().filter(sheet -> ExcelReader.getRowsFromSheet(sheet).stream().filter(Objects::nonNull)
            .limit(15)
            .anyMatch(s -> getRowOfDataAsStrings(s, list.size() - 1)
                .stream()
                .anyMatch(k -> list.stream().anyMatch(p -> p.equalsIgnoreCase(k)))))
        .toList();

  }

  public List<Cell> getRowOfDataAsCells(Row row, int endColumnIndex) {
    List<Cell> data = new ArrayList<>();
    int i = 0;

    while (i <= endColumnIndex) {
      data.add(row.getCell(i));
      i++;
    }
    return data;
  }

  public List<String> getRowOfDataAsStrings(Row row, int endColumnIndex) {

    List<String> data = new ArrayList<>();
    int i = 0;

    while (i <= endColumnIndex) {
      Cell dataCell = row.getCell(i);
      data.add(dataCell != null ? returnStringFromCell(dataCell) : null);
      i++;
    }
    return data;
  }

  public CellAddress findColumnPosition(String key, Row indexRow, int size) {
    Cell cell = null;
    int i = 0;
    while (cell == null
        || (cell.getAddress().getColumn() < indexRow.getLastCellNum() || cell.getAddress().getColumn() < size) && i < indexRow.getLastCellNum()) {
      cell = indexRow.getCell(i);
      if (cell != null && cell.getRichStringCellValue().toString().equals(key)) {
        return cell.getAddress();
      }
      i++;
    }
    return null;
  }

  public List<String> getRowOfDataAsStrings(List<Row> rows,
      RowAndCellAddress rowAndCellStartAddress,
      int endColumnIndex) {

    List<String> data = new ArrayList<>();
    rows = rows.stream().filter(Objects::nonNull).toList();

    List<Row> rowsFiltered = rows.stream().filter(row -> row.getRowNum() >= rowAndCellStartAddress.row.getRowNum()).toList();
    Iterator<Row> rowIterator = rowsFiltered.iterator();
    Row row = rowIterator.next();

    Iterator<Cell> cellIterator = row.cellIterator();
    Cell dataCell = null;
    while (cellIterator.hasNext() && (dataCell == null
        || endColumnIndex >= dataCell.getColumnIndex())) {
      dataCell = cellIterator.next();
      if (dataCell.getAddress().getColumn() >= rowAndCellStartAddress.cellAddress.getColumn()) {
        data.add(returnStringFromCell(dataCell));
      }
    }
    return data;
  }

  public List<RowAndCellAddress> getRowAndCellAddressesForString(List<Row> rows, String matchingString) {
    return getRowAndCellAddressesForString(rows, matchingString, false);
  }

  public List<RowAndCellAddress> getRowAndCellAddressesForString(List<Row> rows, String matchingString, boolean exactMatch) {
    List<Row> matchedRows = new ArrayList<>();
    List<CellAddress> cellAddresses = new ArrayList<>();

    for (Row row : rows) {
      if (row != null) {
        var cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
          var cell = cellIterator.next();
          if (exactMatch
              ? returnStringFromCell(cell).equalsIgnoreCase(matchingString)
              : returnStringFromCell(cell).toLowerCase().contains(matchingString.toLowerCase())) {
            cellAddresses.add(cell.getAddress());
            matchedRows.add(row);
          }
        }
      }
    }

    List<RowAndCellAddress> rowAndCellAddresses = new ArrayList<>();

    for (Row row : matchedRows) {
      rowAndCellAddresses.add(new RowAndCellAddress(row, cellAddresses.get(matchedRows.indexOf(row))));
    }

    return rowAndCellAddresses;
  }

  public static Optional<RowAndCellAddress> getRowAndCellAddressForString(List<Row> rows, String matchingString) {
    return getRowAndCellAddressForString(rows, matchingString, true);
  }

  public static Optional<RowAndCellAddress> getRowAndCellAddressForString(List<Row> rows, String matchingString, boolean exactMatch) {
    // Will look for cell with matching cell value.
    for (Row row : rows) {
      if (row != null) {
        var cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
          var cell = cellIterator.next();
          if (exactMatch
              ? returnStringFromCell(cell).equalsIgnoreCase(matchingString)
              : returnStringFromCell(cell).toLowerCase().contains(matchingString.toLowerCase())) {
            // mapping to correct index, but we'll jump up one row to find the correct starting position.
            return Optional.of(new RowAndCellAddress(row, new CellAddress(cell.getAddress().getRow(), cell.getColumnIndex())));
          }
        }
      }
    }
    return Optional.empty();
  }

  public record RowAndCellAddress(Row row, CellAddress cellAddress) {

  }

  public List<String> getCellValuesFromRow(Row row) {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < row.getLastCellNum(); i++) {
      values.add(getCellValue(row.getCell(i)));
    }
    return values;
  }

  public String getCellValue(Cell cell) {
    if (cell != null) {
      return returnStringFromCell(cell);
    } else {
      return "";
    }
  }

  /**
   * Function to read the string value or numericValue from a Cell
   * Will cast it to a XSSFCell to be able to detect is a cell with a strikethrough.
   *
   * @param cell Cell
   * @return String either with value or " ", if it's not numeric or String and without strikethrough
   */
  public static String returnStringFromCell(Cell cell) {
    DataFormatter formatter = new DataFormatter();
    XSSFCell transformedCell = (XSSFCell) cell;
    if (!transformedCell.getCellStyle().getFont().getStrikeout()) {
      switch (transformedCell.getCellType()) {
        case STRING -> {
          return transformedCell.getStringCellValue();
        }
        case NUMERIC -> {
          if (DateUtil.isCellDateFormatted(cell)) {
            // Save it in ISO 8601 standard
            formatter.addFormat("m/d/yy", new SimpleDateFormat("yyyy-MM-dd"));
            return formatter.formatCellValue(transformedCell);
          } else {
            return formatter.formatCellValue(transformedCell);
          }
        }
        default -> {
          return " ";
        }
      }
    }
    return " ";
  }


}
