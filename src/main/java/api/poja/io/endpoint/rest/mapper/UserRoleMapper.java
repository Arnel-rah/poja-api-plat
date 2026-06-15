package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.UserRoleEnum;
import api.poja.io.endpoint.rest.security.model.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserRoleMapper {
  public UserRoleEnum toRest(UserRole userRole) {
    return switch (userRole) {
      case USER -> UserRoleEnum.USER;
      case ADMIN -> UserRoleEnum.ADMIN;
    };
  }
}
