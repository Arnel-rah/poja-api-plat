package api.poja.io.service.validator;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.service.UserService;
import api.poja.io.service.UserSubscriptionService;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserAppThresholdValidator implements BiConsumer<String, List<String>> {

  private final long normalUsersThreshold;
  private final long endToEndTestThreshold;
  private final long betaUserThreshold;
  private final ApplicationRepository applicationRepository;
  private final UserSubscriptionService userSubscriptionService;
  private final UserService userService;

  public UserAppThresholdValidator(
      @Value("${max.apps.per.user}") long maxAppsNbPerUser,
      @Value("${e2e.max.apps.per.user}") long endToEndTestMaxAppsNbPerUser,
      @Value("${beta.user.app.threshold}") long betaUserAppThreshold,
      ApplicationRepository applicationRepository,
      UserSubscriptionService userSubscriptionService,
      UserService userService) {
    this.normalUsersThreshold = maxAppsNbPerUser;
    this.endToEndTestThreshold = endToEndTestMaxAppsNbPerUser;
    this.betaUserThreshold = betaUserAppThreshold;
    this.applicationRepository = applicationRepository;
    this.userSubscriptionService = userSubscriptionService;
    this.userService = userService;
  }

  @Override
  public void accept(String userId, List<String> applicationIdsToSave) {
    long threshold = getUserAppThreshold(userId);
    checkThreshold(threshold, userId, applicationIdsToSave);
  }

  private long getUserAppThreshold(String userId) {
    var user = userService.getUserById(userId);
    var subscriptionOpt = userSubscriptionService.findActiveSubscriptionByUserId(user.getId());
    return subscriptionOpt
        .map(s -> s.getOffer().getMaxApps())
        .orElseGet(
            () -> {
              if (user.isEndToEndTestUser()) {
                return endToEndTestThreshold;
              }
              if (user.isBetaTester()) {
                return betaUserThreshold;
              }
              return normalUsersThreshold;
            });
  }

  private void checkThreshold(
      long maxAppsPerUser, String userId, List<String> applicationIdsToSave) {
    long nbOfActiveAppsPerUser = applicationRepository.countByUserIdAndArchived(userId, false);
    if (nbOfActiveAppsPerUser > maxAppsPerUser) {
      throw new BadRequestException(
          "cannot have more than " + maxAppsPerUser + " non archived apps per user");
    }
    long appsToCreate =
        applicationIdsToSave.stream()
            .filter(Objects::nonNull)
            .map(applicationRepository::existsById)
            .filter(exists -> !exists)
            .count();
    if (nbOfActiveAppsPerUser + appsToCreate > maxAppsPerUser) {
      throw new BadRequestException(
          "cannot have more than "
              + maxAppsPerUser
              + " non archived apps per user. you have "
              + nbOfActiveAppsPerUser
              + " non archived apps");
    }
  }
}
