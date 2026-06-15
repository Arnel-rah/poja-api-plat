package api.poja.io.model;

import java.time.LocalDate;
import java.time.YearMonth;

public record DateInterval(LocalDate start, LocalDate end) {
  public DateInterval {
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("Start cannot be after end.");
    }
  }

  public static DateInterval of(LocalDate start, LocalDate end) {
    return new DateInterval(start, end);
  }

  public static DateInterval from(YearMonth period) {
    return of(period.atDay(1), period.atEndOfMonth());
  }

  public software.amazon.awssdk.services.costexplorer.model.DateInterval toAwsDateInterval() {
    return software.amazon.awssdk.services.costexplorer.model.DateInterval.builder()
        .start(start.toString())
        .end(end.toString())
        .build();
  }
}
