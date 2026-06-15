package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.UserCreated;
import api.poja.io.mail.jet.MailjetComponent;
import api.poja.io.service.UserService;
import api.poja.io.sys.platform.SaasOnly;
import com.mailjet.client.transactional.SendContact;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
@SaasOnly
public class UserCreatedService implements Consumer<UserCreated> {

  private final UserService userService;
  private final MailjetComponent mailjetComponent;

  @Override
  public void accept(UserCreated userCreated) {
    var userId = userCreated.getUserId();
    var userOpt = userService.findById(userId);

    if (userOpt.isEmpty()) {
      log.error("User.id={} not found for account creation notification", userId);
      return;
    }

    var user = userOpt.get();
    var to = new SendContact(user.getEmail(), user.getUsername());
    log.info(
        "Notifying User(id={},email={}) about account creation", user.getId(), user.getEmail());

    mailjetComponent.sendWithTemplateTo(mailjetComponent.conf().accountCreatedTemplateId(), to);
  }
}
