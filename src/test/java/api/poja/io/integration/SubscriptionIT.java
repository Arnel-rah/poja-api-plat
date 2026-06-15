package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.PREMIUM_OFFER_ID;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.SubscriptionApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.SubscribeRequestBody;
import api.poja.io.endpoint.rest.model.UserSubscription;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.repository.jpa.InvoiceRepository;
import api.poja.io.repository.model.Invoice;
import api.poja.io.repository.model.enums.InvoiceStatus;
import api.poja.io.service.InvoiceService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class SubscriptionIT extends MockedThirdParties {
  @MockBean InvoiceService invoiceServiceMock;
  @Autowired InvoiceRepository invoiceRepository;

  private static void assert_jane_doe_can_only_unsub_once(
      SubscriptionApi janeDoeApi, UserSubscription janeDoeSub, String offerId) throws ApiException {
    janeDoeApi.unsubscribe(JANE_DOE_ID, janeDoeSub.getId());
    assertThrowsBadRequestException(
        () -> janeDoeApi.unsubscribe(JANE_DOE_ID, janeDoeSub.getId()),
        "subscription.id=" + janeDoeSub.getId() + " for offer.id=" + offerId + " already ended");
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
    when(invoiceServiceMock.refreshInvoice(any())).thenCallRealMethod();
    when(invoiceServiceMock.save(any()))
        .thenAnswer(
            (i) -> {
              var invoice = i.getArgument(0, Invoice.class);
              return invoiceRepository.save(invoice.toBuilder().status(InvoiceStatus.PAID).build());
            });
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @Test
  void subscribe_and_unsubscribe_ok() throws ApiException {
    var janeDoe = anApiClient(JANE_DOE_TOKEN);
    SubscriptionApi janeDoeApi = new SubscriptionApi(janeDoe);
    var joeDoe = anApiClient(JOE_DOE_TOKEN);
    SubscriptionApi joeDoeApi = new SubscriptionApi(joeDoe);

    var janeDoeSub =
        assert_jane_doe_can_sub_only_once_and_joe_doe_cant_sub_due_to_offer_limit(
            janeDoeApi, PREMIUM_OFFER_ID, joeDoeApi);
    assert_jane_doe_can_only_unsub_once(janeDoeApi, janeDoeSub, PREMIUM_OFFER_ID);
    assert_jane_doe_can_resub_and_reunsub(janeDoeApi, PREMIUM_OFFER_ID);
  }

  @Test
  void unsub_non_subscribed_ko() {
    var joeDoe = anApiClient(JOE_DOE_TOKEN);
    SubscriptionApi joeDoeApi = new SubscriptionApi(joeDoe);
    assertThrowsForbiddenException(
        () -> joeDoeApi.unsubscribe(JANE_DOE_ID, "mock_id"), "Access Denied");
  }

  private void assert_jane_doe_can_resub_and_reunsub(SubscriptionApi janeDoeApi, String offerId)
      throws ApiException {
    var janeDoeSub2 =
        janeDoeApi.subscribe(JANE_DOE_ID, new SubscribeRequestBody().offerId(offerId));
    assert_jane_doe_can_read_subs(janeDoeApi, janeDoeSub2);
    janeDoeApi.unsubscribe(JANE_DOE_ID, janeDoeSub2.getId());
  }

  private @NotNull UserSubscription
      assert_jane_doe_can_sub_only_once_and_joe_doe_cant_sub_due_to_offer_limit(
          SubscriptionApi janeDoeApi, String offerId, SubscriptionApi joeDoeApi)
          throws ApiException {
    var janeDoeSub = janeDoeApi.subscribe(JANE_DOE_ID, new SubscribeRequestBody().offerId(offerId));
    assertThrowsBadRequestException(
        () -> janeDoeApi.subscribe(JANE_DOE_ID, new SubscribeRequestBody().offerId(offerId)),
        "User.Id=" + JANE_DOE_ID + " is already subscribed to an offer.");
    assertThrowsBadRequestException(
        () -> joeDoeApi.subscribe(JOE_DOE_ID, new SubscribeRequestBody().offerId(offerId)),
        "Offer.Id=" + offerId + " cannot be subscribed anymore, limit was reached.");
    assert_jane_doe_can_read_subs(janeDoeApi, janeDoeSub);
    return janeDoeSub;
  }

  private void assert_jane_doe_can_read_subs(SubscriptionApi janeDoeApi, UserSubscription expected)
      throws ApiException {
    var inactives = janeDoeApi.getUserSubscriptions(JANE_DOE_ID, false);
    var actives = janeDoeApi.getUserSubscriptions(JANE_DOE_ID, true);
    var all = janeDoeApi.getUserSubscriptions(JANE_DOE_ID, null);
    var byId = janeDoeApi.getUserSubscription(JANE_DOE_ID, expected.getId());

    assertEquals(expected, byId);
    assertTrue(requireNonNull(actives.getData()).contains(expected));
    assertTrue(
        requireNonNull(actives.getData()).stream().allMatch(sub -> TRUE.equals(sub.getIsActive())));
    assertTrue(all.getData().containsAll(actives.getData()));
    assertTrue(all.getData().containsAll(inactives.getData()));
    assertTrue(
        requireNonNull(inactives.getData()).stream()
            .allMatch(sub -> FALSE.equals(sub.getIsActive())));
  }
}
