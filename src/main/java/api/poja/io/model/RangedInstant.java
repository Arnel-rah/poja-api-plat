package api.poja.io.model;

import static java.time.ZoneOffset.UTC;

import api.poja.io.endpoint.rest.model.MonthType;
import java.time.Instant;
import java.time.Month;
import java.time.YearMonth;
import java.util.Optional;

public record RangedInstant(Instant start, Instant end) {

  public static RangedInstant getRangedInstant(
      Instant startTime, Instant endTime, Integer year, MonthType month) {
    YearMonth yearMonthFromParams =
        (year == null || month == null)
            ? YearMonth.now()
            : YearMonth.of(year, Month.valueOf(month.getValue()));
    var start =
        Optional.ofNullable(startTime)
            .orElseGet(() -> yearMonthFromParams.atDay(1).atStartOfDay(UTC).toInstant());
    var end = Optional.ofNullable(endTime).orElseGet(() -> getEndOfMonthOrNow(yearMonthFromParams));
    return new RangedInstant(start, end);
  }

  public static RangedInstant getRangedInstant(YearMonth yearMonth) {
    var start = yearMonth.atDay(1).atStartOfDay(UTC).toInstant();
    var end = getEndOfMonthOrNow(yearMonth);
    return new RangedInstant(start, end);
  }

  private static Instant getEndOfMonthOrNow(YearMonth yearMonth) {
    Instant now = Instant.now();
    var endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(UTC);
    if (now.isBefore(endOfMonth)) {
      return now;
    }
    return endOfMonth;
  }
}
