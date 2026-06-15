package api.poja.io.service.validator;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.service.UserSuspensionService;
import api.poja.io.service.user.UserSuspensionConf;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserSuspensionValidator implements BiConsumer<String, String> {
  private final UserSuspensionConf userSuspensionConf;
  private final UserSuspensionService service;

  @Override
  public void accept(String userId, String suspensionReason) {
    var optionalSuspension =
        service.findByRecentByUserIdAndSuspensionReason(userId, suspensionReason);
    if (optionalSuspension.isPresent()) {
      var suspension = optionalSuspension.get();
      long remainingDays = service.computeRemainingSuspensionGracePeriodDays(suspension);
      throw new BadRequestException(
          String.format(
              "User.id=%s cannot be suspended for the same reason again until %d days.",
              userId, remainingDays));
    }
  }
}
