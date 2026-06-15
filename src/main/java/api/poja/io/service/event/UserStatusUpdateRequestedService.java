package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.UserStatusUpdateRequested;
import api.poja.io.service.userStatusUpdate.UserActivationRequestedService;
import api.poja.io.service.userStatusUpdate.UserSuspensionRequestedService;
import api.poja.io.sys.platform.SaasOnly;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@SaasOnly
public class UserStatusUpdateRequestedService implements Consumer<UserStatusUpdateRequested> {
  private final UserSuspensionRequestedService userSuspensionRequestedService;
  private final UserActivationRequestedService userActivationRequestedService;

  @Override
  @Transactional
  public void accept(UserStatusUpdateRequested userStatusUpdateRequested) {
    UserStatusUpdateRequested.StatusAlteration status = userStatusUpdateRequested.getStatus();
    switch (status) {
      case SUSPEND -> userSuspensionRequestedService.accept(userStatusUpdateRequested);
      case ACTIVATE -> userActivationRequestedService.accept(userStatusUpdateRequested);
    }
  }
}
