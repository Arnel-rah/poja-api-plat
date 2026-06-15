package api.poja.io.model.cost;

import api.poja.io.model.DateInterval;
import java.math.BigDecimal;
import java.util.Objects;

public record CostByTime(DateInterval timePeriod, BigDecimal amount, String unit) {
  private static final String USD = "USD";

  public CostByTime {
    if (!Objects.equals(USD, unit)) {
      throw new IllegalArgumentException("Only USD is supported as unit");
    }
  }

  public CostByTime(DateInterval timePeriod, BigDecimal amount) {
    this(timePeriod, amount, USD);
  }
}
