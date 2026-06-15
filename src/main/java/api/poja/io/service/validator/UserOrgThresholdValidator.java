package api.poja.io.service.validator;

import api.poja.io.model.User;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.jpa.OrganizationRepository;
import api.poja.io.service.UserService;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserOrgThresholdValidator implements BiConsumer<String, List<String>> {
  public UserOrgThresholdValidator(
      @Value("${max.orgs.per.user}") long normalUsersThreshold,
      @Value("${e2e.max.orgs.per.user}") long e2eUsersThreshold,
      @Value("${beta.max.user.orgs.per.user}") long betaUsersThreshold,
      @Value("${e2e.user.id}") String endToEndTestUserId,
      OrganizationRepository organizationRepository,
      UserService userService) {
    this.normalUsersThreshold = normalUsersThreshold;
    this.endToEndTestThreshold = e2eUsersThreshold;
    this.betaUsersThreshold = betaUsersThreshold;
    this.endToEndTestUserId = endToEndTestUserId;
    this.organizationRepository = organizationRepository;
    this.userService = userService;
  }

  private final long normalUsersThreshold;
  private final long endToEndTestThreshold;
  private final long betaUsersThreshold;
  private final String endToEndTestUserId;
  private final OrganizationRepository organizationRepository;
  private final UserService userService;

  public void accept(String userId, List<String> organizationIdsToSave) {
    if (endToEndTestUserId.equals(userId)) {
      checkThreshold(endToEndTestThreshold, userId, organizationIdsToSave);
      return;
    }
    User user = userService.getUserById(userId);
    if (user.isBetaTester()) {
      checkThreshold(betaUsersThreshold, userId, organizationIdsToSave);
      return;
    }
    checkThreshold(normalUsersThreshold, userId, organizationIdsToSave);
  }

  private void checkThreshold(
      long maxOrgsPerUser, String userId, List<String> organizationIdsToSave) {
    long nbOfOrgsPerUser = organizationRepository.countByOwnerId(userId);
    if (nbOfOrgsPerUser > maxOrgsPerUser) {
      throw new BadRequestException(
          "cannot have more than " + maxOrgsPerUser + " organizations per user");
    }
    long orgsToSave =
        organizationIdsToSave.stream()
            .filter(Objects::nonNull)
            .map(organizationRepository::existsById)
            .filter(exists -> !exists)
            .count();
    if (nbOfOrgsPerUser + orgsToSave > maxOrgsPerUser) {
      throw new BadRequestException(
          "cannot have more than " + maxOrgsPerUser + " organizations per user");
    }
  }
}
