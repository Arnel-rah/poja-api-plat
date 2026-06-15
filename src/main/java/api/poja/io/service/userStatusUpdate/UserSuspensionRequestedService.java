package api.poja.io.service.userStatusUpdate;

import static api.poja.io.endpoint.event.model.ConsoleUserStatusUpdateRequested.StatusAlteration.SUSPEND;
import static api.poja.io.model.UserStatus.SUSPENDED;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppStatusUpdateRequested;
import api.poja.io.endpoint.event.model.ConsoleUserStatusUpdateRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.UserStatusUpdateRequested;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.UserSuspension;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.UserService;
import api.poja.io.service.UserSuspensionService;
import api.poja.io.service.workflows.userState.UserStateService;
import api.poja.io.sys.platform.SaasOnly;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@SaasOnly
public class UserSuspensionRequestedService implements Consumer<UserStatusUpdateRequested> {
  private final ApplicationService applicationService;
  private final UserSuspensionService userSuspensionService;
  private final EventProducer<PojaEvent> eventProducer;
  private final UserService userService;
  private final UserStateService userStateService;

  private static List<PojaEvent> getSuspensionEvents(
      UserStatusUpdateRequested userStatusUpdateRequested, List<Application> apps) {
    var appSuspensionEvents = getAppSuspensionEvents(userStatusUpdateRequested, apps);
    var consoleUserSuspensionEvent =
        getConsoleUserSuspensionEvent(userStatusUpdateRequested.getUserId());

    List<PojaEvent> suspensionEvents = new ArrayList<>(appSuspensionEvents);
    suspensionEvents.add(consoleUserSuspensionEvent);
    return suspensionEvents;
  }

  private static PojaEvent getConsoleUserSuspensionEvent(String userId) {
    return ConsoleUserStatusUpdateRequested.builder().userId(userId).status(SUSPEND).build();
  }

  private static List<PojaEvent> getAppSuspensionEvents(
      UserStatusUpdateRequested userStatusUpdateRequested, List<Application> apps) {
    return apps.stream()
        .map(app -> toAppSuspensionRequested(userStatusUpdateRequested.getUserId(), app))
        .toList();
  }

  private static PojaEvent toAppSuspensionRequested(String userId, Application app) {
    return AppStatusUpdateRequested.builder()
        .status(AppStatusUpdateRequested.StatusAlteration.SUSPEND)
        .userId(userId)
        .appId(app.getId())
        .build();
  }

  @Override
  @SneakyThrows
  public void accept(UserStatusUpdateRequested userStatusUpdateRequested) {
    // TODO: interrupt user suspension with an already used _suspensionReason_ within the grace
    var user = userService.getUserById(userStatusUpdateRequested.getUserId());
    if (userStateService.isLastSuspended(user.getId())) {
      log.info("User.id={} is currently suspended.", user.getId());
      return;
    }
    assert UserStatusUpdateRequested.StatusAlteration.SUSPEND.equals(
        userStatusUpdateRequested.getStatus());

    var optionalSuspensionWithIdenticalReason =
        userSuspensionService.findByRecentByUserIdAndSuspensionReason(
            user.getId(), userStatusUpdateRequested.getStatusReason());
    if (optionalSuspensionWithIdenticalReason.isPresent()) {
      var suspension = optionalSuspensionWithIdenticalReason.get();
      long remainingDays =
          userSuspensionService.computeRemainingSuspensionGracePeriodDays(suspension);
      log.info(
          "User.id={} cannot be suspended for the same reason again until {} days.",
          user.getId(),
          remainingDays);
      return;
    }

    userSuspensionService.save(
        UserSuspension.builder()
            .userId(user.getId())
            .suspensionReason(userStatusUpdateRequested.getStatusReason())
            .suspendedAt(userStatusUpdateRequested.getRequestedAt())
            .build());

    userService.updateUserStatus(
        userStatusUpdateRequested.getUserId(),
        SUSPENDED,
        userStatusUpdateRequested.getStatusReason(),
        userStatusUpdateRequested.getRequestedAt());
    List<Application> apps =
        applicationService.findAllNotArchivedAndNotSuspendedByUserId(
            userStatusUpdateRequested.getUserId());
    eventProducer.accept(getSuspensionEvents(userStatusUpdateRequested, apps));
  }
}
