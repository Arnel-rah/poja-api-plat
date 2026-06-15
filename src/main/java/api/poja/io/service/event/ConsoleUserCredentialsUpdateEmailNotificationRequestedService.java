package api.poja.io.service.event;

import static java.util.Collections.emptyList;

import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateEmailNotificationRequested;
import api.poja.io.mail.Email;
import api.poja.io.mail.Mailer;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import jakarta.mail.internet.InternetAddress;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConsoleUserCredentialsUpdateEmailNotificationRequestedService
    implements Consumer<ConsoleUserCredentialsUpdateEmailNotificationRequested> {
  private final Mailer mailer;
  private final String consoleBaseUrl;
  private final OrganizationService organizationService;
  private final UserService userService;

  private static final String SUBJECT_TEMPLATE =
      "[POJA] Organization %s console login access updated";
  private static final String HTML_BODY_TEMPLATE =
      """
Hello %s,<br><br>

<p>
  This is to inform you that the organization <b>%s</b> console login access credentials have been updated.<br>
  As part of this change, the removed member will no longer have access to the console.<br>
  This update was made for security reasons to ensure that only active members retain access.<br><br>
</p>

<p>
You can view the updated credentials directly in the organization under the <b>Console Login</b> tab,<br>
or access them using the following link: <a href="%s">console login</a><br><br>
</p>

<p>
If you have any questions, please contact the administrator.<br><br>
</p>

Best regards,<br>
Poja Team
""";

  public ConsoleUserCredentialsUpdateEmailNotificationRequestedService(
      Mailer mailer,
      @Value("${poja.console.base.url}") String consoleBaseUrl,
      OrganizationService organizationService,
      UserService userService) {
    this.mailer = mailer;
    this.consoleBaseUrl = consoleBaseUrl;
    this.organizationService = organizationService;
    this.userService = userService;
  }

  @Override
  @SneakyThrows
  public void accept(ConsoleUserCredentialsUpdateEmailNotificationRequested event) {
    var org = organizationService.getById(event.getOrgId());
    var orgName = org.getName();
    var user = userService.getUserById(event.getUserId());
    var to = new InternetAddress(user.getEmail());

    var subject = SUBJECT_TEMPLATE.formatted(orgName);
    var htmlBody =
        HTML_BODY_TEMPLATE.formatted(user.getUsername(), orgName, toConsoleLoginUrl(org));
    mailer.accept(new Email(to, emptyList(), emptyList(), subject, htmlBody, emptyList()));
  }

  private String toConsoleLoginUrl(Organization org) {
    return "%s/organizations/%s/show/console-login".formatted(consoleBaseUrl, org.getId());
  }
}
