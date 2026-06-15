package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.OrganizationMembersMovementTypeEnum.REMOVE;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateRequested;
import api.poja.io.endpoint.rest.model.CrupdateOrganizationMembersRequestBody;
import api.poja.io.model.User;
import api.poja.io.service.organization.OrganizationUsersService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class OrganizationUserServiceTest extends MockedThirdParties {
  @Autowired private OrganizationUsersService subject;

  @Test
  void remove_user_from_org_triggers_console_user_credentials_update() {
    // refactor: complete user mock
    var authenticated = User.builder().id(JOE_DOE_ID).build();

    subject.crupdateOrgMembers(
        authenticated,
        ORG_1_ID,
        List.of(
            new CrupdateOrganizationMembersRequestBody()
                .idUser(LOREM_IPSUM_ID)
                .movementType(REMOVE)));

    verify(eventProducerMock)
        .accept(
            argThat(l -> l.equals(List.of(new ConsoleUserCredentialsUpdateRequested(ORG_1_ID)))));
  }
}
