package api.poja.io.service.event;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppArchivalRequested;
import api.poja.io.endpoint.event.model.OrgArchivalRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.repository.model.Application;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.organization.OrganizationService;
import java.time.Instant;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrgArchivalRequestedService implements Consumer<OrgArchivalRequested> {
  private final EventProducer<PojaEvent> eventProducer;
  private final OrganizationService organizationService;
  private final ApplicationService applicationService;

  @Override
  public void accept(OrgArchivalRequested orgArchivalRequested) {
    var org = organizationService.getById(orgArchivalRequested.getOrgId());
    var apps = applicationService.findAllByOrgIdAndArchived(org.getId(), false);
    var now = Instant.now();

    eventProducer.accept(apps.stream().map(a -> getAppArchivalRequested(a, now)).toList());
    // TODO: delete org console user group(s) ?
  }

  private static PojaEvent getAppArchivalRequested(Application o, Instant requestedAt) {
    return new AppArchivalRequested(o.getOrgId(), o.getId(), requestedAt);
  }
}
