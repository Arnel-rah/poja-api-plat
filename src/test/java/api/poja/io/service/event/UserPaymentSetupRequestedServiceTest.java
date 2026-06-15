package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.COMPLETED;
import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.FAILED;
import static api.poja.io.integration.conf.utils.TestMocks.ADMIN_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.TO_UPSERT_STRIPE_CUSTOMER_ID;
import static api.poja.io.integration.conf.utils.TestMocks.USER_TO_UPSERT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.toUpsertStripeCustomer;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.UserPaymentSetupRequested;
import api.poja.io.endpoint.rest.api.UserApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.model.User;
import api.poja.io.service.UserService;
import com.stripe.exception.ApiException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserPaymentSetupRequestedServiceTest extends MockedThirdParties {
  @Autowired private UserPaymentSetupRequestedService subject;
  @Autowired private UserService userService;
  private static final String TO_UPSERT_2_ID = "to_upsert_id_2";

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @SneakyThrows
  void setupSuccessfulCustomerCreation() {
    when(stripeServiceMock.createCustomer(any(), any())).thenReturn(toUpsertStripeCustomer());
  }

  @SneakyThrows
  void setupFailedCustomerCreation() {
    when(stripeServiceMock.createCustomer(any(), any()))
        .thenThrow(new ApiException("Stripe exception", null, null, 400, null));
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
  }

  @Test
  void user_payment_setup_ok() throws api.poja.io.endpoint.rest.client.ApiException {
    setupSuccessfulCustomerCreation();
    var adminClient = anApiClient(ADMIN_TOKEN);
    var api = new UserApi(adminClient);
    var user = User.builder().id(USER_TO_UPSERT_ID).build();
    var event = new UserPaymentSetupRequested(user);

    subject.accept(event);
    var upsertedUser = userService.getUserById(USER_TO_UPSERT_ID);
    var latestState =
        requireNonNull(api.getUserPaymentSetupStates(USER_TO_UPSERT_ID).getData()).getFirst();

    assertEquals(TO_UPSERT_STRIPE_CUSTOMER_ID, upsertedUser.getStripeId());
    assertEquals(COMPLETED, latestState.getProgressionStatus());
  }

  @Test
  void user_payment_setup_ko() throws api.poja.io.endpoint.rest.client.ApiException {
    setupFailedCustomerCreation();
    var adminClient = anApiClient(ADMIN_TOKEN);
    var api = new UserApi(adminClient);
    var user = User.builder().id(TO_UPSERT_2_ID).build();
    var event = new UserPaymentSetupRequested(user);

    subject.accept(event);
    var upsertedUser = userService.getUserById(TO_UPSERT_2_ID);
    var latestState =
        requireNonNull(api.getUserPaymentSetupStates(TO_UPSERT_2_ID).getData()).getFirst();

    assertNull(upsertedUser.getStripeId());
    assertEquals(FAILED, latestState.getProgressionStatus());
  }
}
