package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.OrganizationInviteType;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import org.springframework.stereotype.Component;

@Component
public class OrganizationInviteStatusMapper {
  public OrganizationInviteType toRest(OrganizationInviteStatus status) {
    return switch (status) {
      case ACCEPTED -> OrganizationInviteType.ACCEPTED;
      case REJECTED -> OrganizationInviteType.REJECTED;
      case PENDING -> OrganizationInviteType.PENDING;
    };
  }
}
