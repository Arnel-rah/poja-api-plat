package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.UserState;
import api.poja.io.endpoint.rest.model.UserStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class UserStateMapper {
  public UserState toRest(api.poja.io.repository.model.UserState domain) {
    return new UserState()
        .id(domain.getId())
        .userId(domain.getUserId())
        .timestamp(domain.getTimestamp())
        .description(domain.getDescription())
        .progressionStatus(UserStatusEnum.fromValue(domain.getProgressionStatus().getValue()))
        .executionType(domain.getExecutionType());
  }
}
