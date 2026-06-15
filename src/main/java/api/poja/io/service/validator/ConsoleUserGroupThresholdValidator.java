package api.poja.io.service.validator;

import api.poja.io.model.OfferDto;
import api.poja.io.model.User;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.jpa.ConsoleUserGroupRepository;
import api.poja.io.service.OfferService;
import api.poja.io.service.UserService;
import api.poja.io.service.UserSubscriptionService;
import api.poja.io.service.symjaService.SymjaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConsoleUserGroupThresholdValidator {
  public ConsoleUserGroupThresholdValidator(
      @Value("${max.orgs.per.user}") int normalUsersMaxOrgNb,
      @Value("${e2e.max.orgs.per.user}") int endToEndUsersMaxOrgNb,
      @Value("${beta.max.user.orgs.per.user}") int betaUsersMaxOrgNb,
      ConsoleUserGroupRepository consoleUserGroupRepository,
      UserService userService,
      OfferService offerService,
      SymjaService symjaService,
      UserSubscriptionService userSubscriptionService) {

    this.normalUsersMaxOrgNb = normalUsersMaxOrgNb;
    this.endToEndUsersMaxOrgNb = endToEndUsersMaxOrgNb;
    this.betaUsersMaxOrgNb = betaUsersMaxOrgNb;

    this.consoleUserGroupRepository = consoleUserGroupRepository;
    this.userService = userService;
    this.offerService = offerService;
    this.symjaService = symjaService;
    this.userSubscriptionService = userSubscriptionService;
  }

  /*
   */
  private final int normalUsersMaxOrgNb;
  private final int endToEndUsersMaxOrgNb;
  private final int betaUsersMaxOrgNb;

  private final ConsoleUserGroupRepository consoleUserGroupRepository;
  private final UserService userService;
  private final UserSubscriptionService userSubscriptionService;
  private final OfferService offerService;
  private final SymjaService symjaService;

  public void accept(String userId) {
    var user = userService.getUserById(userId);
    var offer = getActiveSubscriptionOfferByUserId(user.getId());
    var threshold = computeMaxConsoleUserGroupsNb(offer.maxApps(), getUserMaxOrgNb(user));
    checkThreshold(user.getId(), threshold);
  }

  private OfferDto getActiveSubscriptionOfferByUserId(String userId) {
    return userSubscriptionService
        .findActiveSubscriptionByUserId(userId)
        .map(o -> offerService.getById(o.getOffer().getId()))
        .orElseGet(() -> offerService.getBasicOfferForUser(userId));
  }

  private int computeMaxConsoleUserGroupsNb(long nbApps, int nbOrgs) {
    var logPoliciesNb = symjaService.computeNeededLogPolicies(nbApps, nbOrgs).intValue();
    return symjaService.computeMaxConsoleUserGroups(logPoliciesNb, nbOrgs).intValue();
  }

  private void checkThreshold(String userId, long threshold) {
    long userGroupsNb = consoleUserGroupRepository.countByUserId(userId).orElse(0L);
    if (userGroupsNb + 1 > threshold) {
      throw new BadRequestException("cannot create new console user group.");
    }
  }

  private int getUserMaxOrgNb(User user) {
    if (user.isEndToEndTestUser()) {
      return endToEndUsersMaxOrgNb;
    }
    if (user.isBetaTester()) {
      return betaUsersMaxOrgNb;
    }
    return normalUsersMaxOrgNb;
  }
}
