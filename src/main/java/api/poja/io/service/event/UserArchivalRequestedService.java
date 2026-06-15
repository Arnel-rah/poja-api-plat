package api.poja.io.service.event;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.OrgArchivalRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.UserArchivalRequested;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import java.time.Instant;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserArchivalRequestedService implements Consumer<UserArchivalRequested> {
  private final EventProducer<PojaEvent> eventProducer;
  private final UserService userService;
  private final OrganizationService organizationService;

  @Override
  public void accept(UserArchivalRequested userArchivalRequested) {
    var user = userService.getUserById(userArchivalRequested.getUserId());
    var orgs = organizationService.findAllByOwnerId(user.getId());
    var requestedAt = userArchivalRequested.getRequestedAt();

    eventProducer.accept(orgs.stream().map(o -> getOrgArchivalRequested(o, requestedAt)).toList());
    // TODO: delete console user event ?
  }

  private static PojaEvent getOrgArchivalRequested(Organization o, Instant requestedAt) {
    return new OrgArchivalRequested(o.getId(), requestedAt);
  }
}
