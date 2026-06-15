package api.poja.io.repository.model.enums;

import java.time.Month;
import lombok.Getter;

@Getter
public enum PaymentRequestPeriod {
  JANUARY,
  FEBRUARY,
  MARCH,
  APRIL,
  MAY,
  JUNE,
  JULY,
  AUGUST,
  SEPTEMBER,
  OCTOBER,
  NOVEMBER,
  DECEMBER;

  public Month getCorrespondingJavaMonth() {
    return switch (this) {
      case JANUARY -> Month.JANUARY;
      case FEBRUARY -> Month.FEBRUARY;
      case MARCH -> Month.MARCH;
      case APRIL -> Month.APRIL;
      case MAY -> Month.MAY;
      case JUNE -> Month.JUNE;
      case JULY -> Month.JULY;
      case AUGUST -> Month.AUGUST;
      case SEPTEMBER -> Month.SEPTEMBER;
      case OCTOBER -> Month.OCTOBER;
      case NOVEMBER -> Month.NOVEMBER;
      case DECEMBER -> Month.DECEMBER;
    };
  }

  public static PaymentRequestPeriod from(Month month) {
    return PaymentRequestPeriod.valueOf(month.name());
  }
}
