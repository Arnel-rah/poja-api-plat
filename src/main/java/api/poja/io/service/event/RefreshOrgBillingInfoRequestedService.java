package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.BillingInfoComputeStatus.FINISHED;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshAppBillingInfoRequested;
import api.poja.io.endpoint.event.model.RefreshOrgBillingInfoRequested;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.BillingInfoService;
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
public class RefreshOrgBillingInfoRequestedService
    implements Consumer<RefreshOrgBillingInfoRequested> {
  private final EventProducer<RefreshAppBillingInfoRequested> eventProducer;
  private final ApplicationService applicationService;
  private final BillingInfoService billingInfoService;
  private final UserStatusEventProducerService statusEventProducerService;

  @Override
  public void accept(RefreshOrgBillingInfoRequested refreshOrgBillingInfoRequested) {
    String userId = refreshOrgBillingInfoRequested.getUserId();
    String orgId = refreshOrgBillingInfoRequested.getOrgId();
    LocalDate startDate = refreshOrgBillingInfoRequested.getLocalDate();

    log.info(
        "RefreshOrgBillingInfoRequested for user {}, org {}, startDate {}",
        userId,
        orgId,
        startDate);

    var apps =
        applicationService.findAllToComputeBillingForByOrgId(
            orgId,
            refreshOrgBillingInfoRequested.getPricingCalculationRequestEndTime(),
            startDate.minusDays(1));
    if (apps.isEmpty()) {
      log.info("No apps found for orgId#{}, startDate={}", orgId, startDate);

      Instant now = now();
      billingInfoService.crupdateBillingInfo(
          BillingInfo.builder()
              .id(refreshOrgBillingInfoRequested.getId().toString())
              .userId(userId)
              .orgId(orgId)
              .computationIntervalEnd(
                  refreshOrgBillingInfoRequested.getPricingCalculationRequestEndTime())
              .computeDatetime(now)
              .computedPrice(ZERO)
              .computedMemoryDurationInMbMinutes(ZERO)
              .status(FINISHED)
              .appId(null)
              .envId(null)
              .queryId(null)
              .pricingMethod(refreshOrgBillingInfoRequested.getPricingMethod())
              .build());
      statusEventProducerService.fireUserStatusRefreshEvent(now, userId);
      return;
    }
    eventProducer.accept(
        apps.stream()
            .map(a -> toRefreshAppBillingInfoRequested(a, refreshOrgBillingInfoRequested))
            .toList());
  }

  private static RefreshAppBillingInfoRequested toRefreshAppBillingInfoRequested(
      Application application, RefreshOrgBillingInfoRequested parent) {
    return new RefreshAppBillingInfoRequested(application.getUserId(), application.getId(), parent);
  }
}
