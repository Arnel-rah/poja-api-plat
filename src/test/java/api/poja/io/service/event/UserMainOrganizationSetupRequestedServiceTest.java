package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.USER_TO_UPSERT_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.UserMainOrganizationSetupRequested;
import api.poja.io.model.User;
import api.poja.io.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserMainOrganizationSetupRequestedServiceTest extends MockedThirdParties {
  @Autowired private UserMainOrganizationSetupRequestedService subject;
  @Autowired private UserService userService;

  @Test
  void user_main_org_setup_ok() {
    var user = User.builder().id(USER_TO_UPSERT_ID).username("ToUpsert").build();
    var event = new UserMainOrganizationSetupRequested(user);

    subject.accept(event);
    var upsertedUser = userService.getUserById(USER_TO_UPSERT_ID);

    assertNotNull(upsertedUser.getMainOrgId());
  }
}
