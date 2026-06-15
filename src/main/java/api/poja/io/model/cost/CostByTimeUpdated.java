package api.poja.io.model.cost;

import api.poja.io.model.DateInterval;
import java.math.BigDecimal;
import java.time.Instant;

public record CostByTimeUpdated(DateInterval timePeriod, BigDecimal amount, Instant updatedAt) {}
