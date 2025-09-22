package org.matsi.excelcreator;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TestObject3 {

  @JsonAlias("my name is")
  private String name;
  @JsonAlias("number and such")
  private int number;
  @JsonAlias("other Things")
  private String otherThings;
  @JsonAlias("array Of Strings")
  private List<String> arrayOfStrings;


}
