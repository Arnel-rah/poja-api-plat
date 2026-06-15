package api.poja.io.service;

import static api.poja.io.model.UserStatus.UNDER_MODIFICATION;
import static api.poja.io.service.event.RefreshUserStatusRequestedService.THIRTY_DAYS;
import static api.poja.io.service.workflows.userState.UserStateService.COMPUTING_USER_STATE_REASON;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshUserStatusRequested;
import api.poja.io.service.workflows.userState.UserStateService;
import api.poja.io.sys.platform.SaasOnly;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@SaasOnly
public class UserStatusEventProducerService {

  private final EventProducer<RefreshUserStatusRequested> eventProducer;
  private final UserService userService;
  private final UserStateService userStateService;

  @SneakyThrows
  public void fireUserStatusRefreshEvent(Instant computationRequestEndDatetime, String userId) {
    if (userStateService.isTransitional(userId)) {
      log.info("User.id={} status is currently under modification.", userId);
      return;
    }
    // 'computationRequestEndDatetime' is set at the start of the billing-info
    // event chain. Using it later for state transitions is likely to trigger an
    // `IllegalOlderStateTransitionException`
    userService.updateUserStatus(userId, UNDER_MODIFICATION, COMPUTING_USER_STATE_REASON, now());
    log.info("fire user status refresh event for {}", userId);
    eventProducer.accept(
        List.of(
            RefreshUserStatusRequested.builder()
                .pricingCalculationRequestStartTime(
                    computationRequestEndDatetime.atZone(UTC).minus(THIRTY_DAYS).toInstant())
                .userId(userId)
                .pricingCalculationRequestEndTime(computationRequestEndDatetime)
                .build()));
  }
}
