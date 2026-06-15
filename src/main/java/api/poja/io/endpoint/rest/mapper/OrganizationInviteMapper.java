package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.OrganizationInvite;
import api.poja.io.endpoint.rest.model.UpdateOrganizationInviteRequestBody;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrganizationInviteMapper {
  private final OrganizationInviteStatusMapper statusMapper;

  public OrganizationInvite toRest(api.poja.io.repository.model.OrganizationInvite domain) {
    return new OrganizationInvite()
        .id(domain.getId())
        .userId(domain.getInvitedUser())
        .orgId(domain.getInviterOrg())
        .invitedAt(domain.getCreationDatetime())
        .type(statusMapper.toRest(domain.getStatus()));
  }

  public api.poja.io.repository.model.OrganizationInvite toDomain(
      UpdateOrganizationInviteRequestBody rest) {
    var status =
        OrganizationInviteStatus.valueOf(Objects.requireNonNull(rest.getType()).getValue());
    return api.poja.io.repository.model.OrganizationInvite.builder()
        .id(rest.getId())
        .status(status)
        .build();
  }
}
