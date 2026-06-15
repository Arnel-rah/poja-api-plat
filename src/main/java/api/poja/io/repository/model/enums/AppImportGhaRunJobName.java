package api.poja.io.repository.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AppImportGhaRunJobName {
  PRE_TRANSFORMATION_TEST("PRE_TRANSFORMATION_TEST"),
  POST_TRANSFORMATION_TEST("POST_TRANSFORMATION_TEST"),
  POST_TRANSFORMATION_PING_TEST("POST_TRANSFORMATION_PING_TEST"),
  ;

  private final String value;

  public static AppImportGhaRunJobName fromValue(String value) {
    for (AppImportGhaRunJobName b : values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }

    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
