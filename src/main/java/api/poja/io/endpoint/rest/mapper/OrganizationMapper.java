package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.model.OrganizationDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class OrganizationMapper {
  public Organization toRest(api.poja.io.repository.model.Organization domain) {
    return new Organization()
        .id(domain.getId())
        .name(domain.getName())
        .creationDatetime(domain.getCreationDatetime())
        .ownerId(domain.getOwnerId());
  }

  public Organization toRest(OrganizationDTO domain) {
    long membersCount = domain.acceptedAndPendingMembersCount();
    return new Organization()
        .id(domain.id())
        .name(domain.name())
        .creationDatetime(domain.creationDatetime())
        .ownerId(domain.ownerId())
        .canInviteMoreUser(true)
        .membersCount(membersCount);
  }

  public api.poja.io.repository.model.Organization toDomain(Organization rest, String userId) {
    return api.poja.io.repository.model.Organization.builder()
        .id(rest.getId())
        .name(rest.getName())
        .creationDatetime(rest.getCreationDatetime())
        .ownerId(userId)
        .build();
  }
}
