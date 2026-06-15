package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.ApplicationCreationFailed;
import api.poja.io.service.ApplicationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ApplicationCreationFailedService implements Consumer<ApplicationCreationFailed> {
  private final ApplicationService applicationService;

  @Override
  public void accept(ApplicationCreationFailed event) {
    try {
      applicationService.deleteAppEnvDeplByAppId(event.getOrgId(), event.getAppId());
      log.info("Cleanup completed");
    } catch (Exception e) {
      log.error(
          "Cleanup failed for orgId={}, appId={}: {}",
          event.getOrgId(),
          event.getAppId(),
          e.getMessage(),
          e);
    }
  }
}
