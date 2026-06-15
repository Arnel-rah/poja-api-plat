package api.poja.io.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserStatus {
  ACTIVE("ACTIVE"),
  SUSPENDED("SUSPENDED"),
  UNDER_MODIFICATION("UNDER_MODIFICATION"),
  UNKNOWN("UNKNOWN");

  private final String value;

  @Override
  public String toString() {
    return value;
  }

  public boolean isTransitional() {
    return UNDER_MODIFICATION.equals(this);
  }
}
