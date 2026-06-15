package api.poja.io.service.event;

import static java.util.Collections.emptyList;

import api.poja.io.endpoint.event.model.OrgInvitationEmailNotificationRequested;
import api.poja.io.mail.Email;
import api.poja.io.mail.Mailer;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import jakarta.mail.internet.InternetAddress;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrgInvitationEmailNotificationRequestedService
    implements Consumer<OrgInvitationEmailNotificationRequested> {
  private final Mailer mailer;
  private final String orgAppInvitationBaseUrl;
  private final OrganizationService organizationService;
  private final UserService userService;

  private static final String SUBJECT =
      """
      [POJA] Invitation to join the %s organization
      """;
  private static final String HTML_BODY =
      """
<center style="font-family: 'Segoe UI', Tahoma, sans-serif;">
  <div style="padding: 32px 40px; border-radius: 12px;  border: 1px solid rgb(0,0,0,.10); text-align: center; max-width: 460px;">
    <h2 style="margin-top: 0; margin-bottom: 12px; color: #1a1a1a; font-size: 24px; letter-spacing: 0.4px; font-weight: 600;">
      Invitation to join the %s organization
    </h2>
    <p style="font-size: 17px; color: #333; margin-top: 0; margin-bottom: 10px;">
      Hello <strong>%s</strong>!,
    </p>
    <p style="margin: 0 0 20px 0; font-size: 15px; color: #555; line-height: 1.6;">
      You have been invited to join the <strong>%s</strong> organization.
    </p>
    <a href="%s/organizations/%s/invitations?userId=%s"
       style="display: inline-block; background-color:  rgb(18, 18, 18); color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600; font-size: 16px; letter-spacing: 0.4px;">
      Join %s
    </a>
  </div>
</center>
""";

  public OrgInvitationEmailNotificationRequestedService(
      Mailer mailer,
      @Value("${org.app.invitation.base.url}") String orgAppInvitationBaseUrl,
      OrganizationService organizationService,
      UserService userService) {
    this.mailer = mailer;
    this.orgAppInvitationBaseUrl = orgAppInvitationBaseUrl;
    this.organizationService = organizationService;
    this.userService = userService;
  }

  @Override
  @SneakyThrows
  public void accept(OrgInvitationEmailNotificationRequested orgInvitation) {
    var to = new InternetAddress(orgInvitation.getEmail());
    var invitedUser = userService.getUserById(orgInvitation.getInvitedUser());
    var orgName = organizationService.getById(orgInvitation.getInviterOrg()).getName();

    var subject = SUBJECT.formatted(orgName);
    var htmlBody =
        HTML_BODY.formatted(
            orgName,
            invitedUser.getUsername(),
            orgName,
            orgAppInvitationBaseUrl,
            orgInvitation.getInviterOrg(),
            invitedUser.getId(),
            orgName);

    mailer.accept(new Email(to, emptyList(), emptyList(), subject, htmlBody, emptyList()));
  }
}
