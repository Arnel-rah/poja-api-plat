package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.UserPaymentSetupState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserPaymentSetupStateMapper {
  private final UserPaymentSetupStatusEnumMapper statusMapper;

  public UserPaymentSetupState toRest(api.poja.io.repository.model.UserPaymentSetupState domain) {
    var status = statusMapper.toRest(domain.getProgressionStatus());
    return new UserPaymentSetupState()
        .id(domain.getId())
        .executionType(domain.getExecutionType())
        .timestamp(domain.getTimestamp())
        .progressionStatus(status);
  }
}
