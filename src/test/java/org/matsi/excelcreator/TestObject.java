package org.matsi.excelcreator;

import com.fasterxml.jackson.annotation.JsonAlias;
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
  @JsonAlias("other Things")
  private String otherThings;
  @JsonAlias("array Of Strings")
  private List<String> arrayOfStrings;

}
