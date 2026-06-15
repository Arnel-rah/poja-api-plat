package api.poja.io.service.validator;

import api.poja.io.model.exception.ServiceUnavailableException;
import api.poja.io.repository.UserRepository;
import api.poja.io.service.symjaService.SymjaService;
import java.util.function.LongConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UsersThresholdValidator implements LongConsumer {

  private final long maxPremiumUsersNb;
  private final UserRepository userRepository;
  private final SymjaService symjaService;

  public UsersThresholdValidator(
      @Value("${max.premium.subscribers}") long maxPremiumUsersNb,
      UserRepository userRepository,
      SymjaService symjaService) {

    this.maxPremiumUsersNb = maxPremiumUsersNb;
    this.symjaService = symjaService;
    this.userRepository = userRepository;
  }

  @Override
  public void accept(long usersToCreate) {
    checkThreshold(usersToCreate);
  }

  private void checkThreshold(long usersToCreate) {
    long usersCount = userRepository.countAll();
    long maxUsersNb = symjaService.computeMaxUsersGivenPremium(maxPremiumUsersNb).longValue();
    if (usersCount > maxUsersNb || usersCount + usersToCreate > maxUsersNb) {
      throw new ServiceUnavailableException(
          "Cannot add "
              + usersToCreate
              + " users: this would exceed the maximum limit of "
              + maxUsersNb
              + " users (current users: "
              + usersCount
              + ").");
    }
  }
}
