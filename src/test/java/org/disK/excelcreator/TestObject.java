package org.disK.excelcreator;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Builder
public class TestObject {

  private String name;
  private int number;
  private String otherThings;
  private List<String> arrayOfStrings;

}
