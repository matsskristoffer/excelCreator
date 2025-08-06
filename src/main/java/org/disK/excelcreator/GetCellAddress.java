package org.disK.excelcreator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;

public class GetCellAddress {

  public static CellAddress findColumnPosition(String key, Row indexRow, int size) {
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
}
