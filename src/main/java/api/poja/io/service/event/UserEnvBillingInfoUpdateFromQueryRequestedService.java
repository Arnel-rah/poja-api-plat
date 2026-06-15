package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.BillingInfoComputeStatus.FINISHED;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus.COMPLETE;
import static software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus.RUNNING;
import static software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus.SCHEDULED;

import api.poja.io.aws.cloudwatch.CloudwatchComponent;
import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.UserEnvBillingInfoUpdateFromQueryRequested;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.UserStatusEventProducerService;
import api.poja.io.service.pricing.calculator.PricingCalculator;
import api.poja.io.sys.platform.SaasOnly;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.QueryStatus;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResultField;

@Component
@AllArgsConstructor
@Slf4j
@SaasOnly
public class UserEnvBillingInfoUpdateFromQueryRequestedService
    implements Consumer<UserEnvBillingInfoUpdateFromQueryRequested> {
  private final CloudwatchComponent cloudwatchComponent;
  private final PricingCalculator pricingCalculator;
  private final BillingInfoService billingInfoService;
  private final UserStatusEventProducerService statusEventProducerService;

  @Override
  public void accept(UserEnvBillingInfoUpdateFromQueryRequested event) {
    log.info(
        "UserEnvBillingInfoUpdateFromQueryRequested for user {}, app {}, env {}, query {}",
        event.getUserId(),
        event.getParentEvent().getAppId(),
        event.getEnvId(),
        event.getQueryId());

    GetQueryResultsResponse getQueryResultsResponse =
        cloudwatchComponent.getQueryResult(event.getQueryId());
    QueryStatus status = getQueryResultsResponse.status();
    var billingInfo = billingInfoService.getByQueryId(event.getQueryId());
    var pricingMethod = billingInfo.getPricingMethod();
    if (!RUNNING.equals(status) && !SCHEDULED.equals(status) && !COMPLETE.equals(status)) {
      log.info("query with ID {} is {}, please inspect cloudwatch", event.getQueryId(), status);
      // what do we do on unhandled query status?
    }
    if (RUNNING.equals(status)) {
      // fail on purpose so event backs off
      throw new EventConsumptionBackOffException("query is still running");
    }
    if (COMPLETE.equals(status)) {
      log.info("query with ID {} completed successfully", event.getQueryId());
      if (getQueryResultsResponse.hasResults()) {
        List<List<ResultField>> results = getQueryResultsResponse.results();
        if (!results.isEmpty()) {
          List<ResultField> firstLine = results.getFirst();
          assert firstLine.size() == 2;
          var billedMemoryDuration = firstLine.getFirst();
          var computedBilledDuration = firstLine.get(1);
          assert "billedMemoryDuration".equals(billedMemoryDuration.field());
          assert "computedBilledDuration".equals(computedBilledDuration.field());
          var computedPrice =
              pricingCalculator.computePrice(
                  pricingMethod, new BigDecimal(billedMemoryDuration.value()));
          var computedDurationInMinutes = Double.valueOf(computedBilledDuration.value());
          var computedMemoryDurationInMbMinutes = new BigDecimal(billedMemoryDuration.value());
          billingInfoService.updateBillingInfoAfterCalculation(
              FINISHED,
              now(),
              computedDurationInMinutes,
              computedPrice,
              computedMemoryDurationInMbMinutes,
              billingInfo.getId());
          log.info("Successfully completed calculation for billing info {}", billingInfo.getId());
          fireUserStatusRefreshEvent(billingInfo.getComputationIntervalEnd(), event.getUserId());
        } else {
          log.info("Query returned empty result, saving zero billing");
          billingInfoService.updateBillingInfoAfterCalculation(
              FINISHED, now(), 0.0, ZERO, ZERO, billingInfo.getId());
          fireUserStatusRefreshEvent(billingInfo.getComputationIntervalEnd(), event.getUserId());
        }
      } else {
        log.info("query with ID {} has no result", event.getQueryId());
        fireUserStatusRefreshEvent(billingInfo.getComputationIntervalEnd(), event.getUserId());
      }
    }
  }

  private void fireUserStatusRefreshEvent(Instant computationRequestEndDatetime, String userId) {
    statusEventProducerService.fireUserStatusRefreshEvent(computationRequestEndDatetime, userId);
  }
}
