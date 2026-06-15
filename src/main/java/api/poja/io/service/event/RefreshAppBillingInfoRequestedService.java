package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.BillingInfoComputeStatus.FINISHED;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshAppBillingInfoRequested;
import api.poja.io.endpoint.event.model.RefreshEnvBillingInfoRequested;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.UserStatusEventProducerService;
import api.poja.io.sys.platform.SaasOnly;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
@SaasOnly
public class RefreshAppBillingInfoRequestedService
    implements Consumer<RefreshAppBillingInfoRequested> {
  private final EnvironmentService environmentService;
  private final EventProducer<RefreshEnvBillingInfoRequested> eventProducer;
  private final BillingInfoService billingInfoService;
  private final UserStatusEventProducerService statusUpdateRequestedService;

  @Override
  public void accept(RefreshAppBillingInfoRequested event) {
    log.info(
        "RefreshAppBillingInfoRequested for user {}, org {}, app {}",
        event.getUserId(),
        event.getOrgId(),
        event.getAppId());

    LocalDate startDate = event.getLocalDate();
    var envs =
        environmentService.findAllEnvsToComputeBillingForByApplicationId(
            event.getAppId(), event.getPricingCalculationRequestEndTime(), startDate.minusDays(1));
    if (envs.isEmpty()) {
      log.info(
          "No envs found for org#{}, user#{}, app#{}",
          event.getOrgId(),
          event.getUserId(),
          event.getAppId());

      Instant now = now();
      billingInfoService.crupdateBillingInfo(
          BillingInfo.builder()
              .id(event.getId().toString())
              .userId(event.getUserId())
              .computationIntervalEnd(event.getPricingCalculationRequestEndTime())
              .computeDatetime(now)
              .computedPrice(ZERO)
              .computedMemoryDurationInMbMinutes(ZERO)
              .appId(event.getAppId())
              .orgId(event.getOrgId())
              .envId(null)
              .queryId(null)
              .status(FINISHED)
              .pricingMethod(event.getPricingMethod())
              .build());
      statusUpdateRequestedService.fireUserStatusRefreshEvent(now, event.getUserId());
      return;
    }
    var events = envs.stream().map(e -> toRefreshEnvBillingInfoRequested(e, event)).toList();
    eventProducer.accept(events);
  }

  private static RefreshEnvBillingInfoRequested toRefreshEnvBillingInfoRequested(
      Environment environment, RefreshAppBillingInfoRequested parent) {
    return new RefreshEnvBillingInfoRequested(
        environment.getId(), parent.getUserId(), parent.getAppId(), parent);
  }
}
