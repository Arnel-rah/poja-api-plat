package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.COMPLETED;
import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.FAILED;
import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.IN_PROGRESS;

import api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class UserPaymentSetupStatusEnumMapper {

  public UserPaymentSetupStatusEnum toRest(
      api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum domain) {
    return switch (domain) {
      case PAYMENT_SETUP_IN_PROGRESS -> IN_PROGRESS;
      case PAYMENT_SETUP_COMPLETED -> COMPLETED;
      case PAYMENT_SETUP_FAILED -> FAILED;
    };
  }
}
