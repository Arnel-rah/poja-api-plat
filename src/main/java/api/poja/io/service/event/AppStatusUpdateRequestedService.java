package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.ACTIVE;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppStatusUpdateRequested;
import api.poja.io.endpoint.event.model.EnvStatusUpdateRequested;
import api.poja.io.endpoint.rest.model.Application;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.sys.platform.SaasOnly;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@SaasOnly
public class AppStatusUpdateRequestedService implements Consumer<AppStatusUpdateRequested> {
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;
  private final EventProducer<EnvStatusUpdateRequested> eventProducer;

  @Override
  @Transactional
  public void accept(AppStatusUpdateRequested appStatusUpdateRequested) {
    var app = applicationService.getById(appStatusUpdateRequested.getAppId());
    switch (appStatusUpdateRequested.getStatus()) {
      case SUSPEND -> {
        if (Application.StatusEnum.SUSPENDED.equals(app.getStatus())) {
          return;
        }
        var envs =
            environmentService.findAllByApplicationIdAndStatus(
                appStatusUpdateRequested.getAppId(), ACTIVE);
        eventProducer.accept(
            envs.stream()
                .map(
                    e ->
                        toEnvStatusUpdateRequested(
                            appStatusUpdateRequested.getAppId(),
                            e,
                            EnvStatusUpdateRequested.StatusAlteration.SUSPEND))
                .toList());
      }
      case ACTIVATE -> {
        if (Application.StatusEnum.ACTIVE.equals(app.getStatus())) {
          return;
        }
        var envs =
            environmentService.findAllByApplicationIdAndStatus(
                appStatusUpdateRequested.getAppId(),
                api.poja.io.endpoint.rest.model.Environment.StatusEnum.SUSPENDED);
        eventProducer.accept(
            envs.stream()
                .map(
                    e ->
                        toEnvStatusUpdateRequested(
                            appStatusUpdateRequested.getAppId(),
                            e,
                            EnvStatusUpdateRequested.StatusAlteration.ACTIVATE))
                .toList());
      }
    }
  }

  private static EnvStatusUpdateRequested toEnvStatusUpdateRequested(
      String appId,
      Environment environment,
      EnvStatusUpdateRequested.StatusAlteration statusAlteration) {
    return EnvStatusUpdateRequested.builder()
        .appId(appId)
        .envId(environment.getId())
        .status(statusAlteration)
        .build();
  }
}
