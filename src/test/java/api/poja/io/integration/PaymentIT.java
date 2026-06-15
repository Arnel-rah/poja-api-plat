package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_STRIPE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeStripeCustomerWithLocation;
import static api.poja.io.integration.conf.utils.TestMocks.paymentMethod;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static api.poja.io.integration.conf.utils.TestUtils.setUpStripe;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.PaymentApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.Address;
import api.poja.io.endpoint.rest.model.PaymentMethod;
import api.poja.io.integration.conf.utils.TestUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
public class PaymentIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  @SneakyThrows
  private void setupSuccessfulStripe() {
    setUpGithub(githubComponentMock);
    setUpStripe(stripeServiceMock);
    when(stripeServiceMock.updateCustomer(eq(JOE_DOE_STRIPE_ID), any(), any(), any(), any()))
        .thenReturn(joeDoeStripeCustomerWithLocation());
  }

  @SneakyThrows
  private void setupFailingStripe() {
    setUpGithub(githubComponentMock);
    setUpStripe(stripeServiceMock);
    when(stripeServiceMock.updateCustomer(eq(JOE_DOE_STRIPE_ID), any(), any(), any(), any()))
        .thenThrow(
            new com.stripe.exception.ApiException(
                "Stripe exception", null, "invalid_tax_location", 400, null));
  }

  @Test
  void get_payment_methods_ok() throws ApiException {
    setupSuccessfulStripe();

    ApiClient joeDoeClient = anApiClient();
    PaymentApi api = new PaymentApi(joeDoeClient);

    var paymentMethodsResponse = api.getPaymentMethods(JOE_DOE_ID);
    PaymentMethod paymentMethod = requireNonNull(paymentMethodsResponse.getData()).getFirst();
    com.stripe.model.PaymentMethod.Card card = paymentMethod().getCard();

    assertEquals(paymentMethod().getId(), paymentMethod.getId());
    assertEquals(card.getBrand(), paymentMethod.getBrand());
    assertEquals(card.getLast4(), paymentMethod.getLast4());
    assertEquals(paymentMethod().getType(), paymentMethod.getType());
  }

  @Test
  void getPaymentCustomerWithoutDefinedLocation_shouldReturnWith_paymentDetailsLocation()
      throws ApiException {
    setupSuccessfulStripe();

    var api = new PaymentApi(anApiClient());

    var joeDoePaymentCustomer = api.getCustomer(JOE_DOE_ID);

    assertEquals(
        "payment_method_country", requireNonNull(joeDoePaymentCustomer.getAddress()).getCountry());
  }

  @Test
  void updateWithValidLocation_shouldPass() throws ApiException {
    setupSuccessfulStripe();

    var api = new PaymentApi(anApiClient());
    var address = new Address().city("Paris").country("FR").postalCode("123").line1("Rue 123");
    var joeDoePaymentCustomer = api.getCustomer(JOE_DOE_ID);
    joeDoePaymentCustomer.setAddress(address);

    var updatedCustomer = api.updatePaymentCustomer(JOE_DOE_ID, joeDoePaymentCustomer);

    assertEquals(address, updatedCustomer.getAddress());
  }

  @Test
  void updateWithInvalidLocation_shouldThrow() throws ApiException {
    setupFailingStripe();

    var api = new PaymentApi(anApiClient());
    var address = new Address().city("Paris").country("FR").postalCode("123").line1("Rue 123");
    var joeDoePaymentCustomer = api.getCustomer(JOE_DOE_ID);
    joeDoePaymentCustomer.setAddress(address);

    assertThrowsBadRequestException(
        () -> api.updatePaymentCustomer(JOE_DOE_ID, joeDoePaymentCustomer),
        "The provided address is not valid. A valid address is required for tax calculation.");
  }
}
