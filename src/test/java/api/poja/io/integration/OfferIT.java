package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.PREMIUM_OFFER_ID;
import static api.poja.io.integration.conf.utils.TestMocks.SUSPENDED_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.premiumOffer;
import static api.poja.io.integration.conf.utils.TestUtils.anUnauthenticatedApiClient;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.SubscriptionApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.GetOffersResponse;
import api.poja.io.integration.conf.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OfferIT extends MockedThirdParties {

  private static GetOffersResponse getOffersResponse() {
    return new GetOffersResponse().data(List.of(premiumOffer()));
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
  }

  @Test
  void active_user_read_offers_ok() throws ApiException {
    var joeDoe = anApiClient(JOE_DOE_TOKEN);
    SubscriptionApi api = new SubscriptionApi(joeDoe);

    GetOffersResponse subscriptions = api.getOffers();
    var offer = api.getOfferById(PREMIUM_OFFER_ID);

    assertEquals(getOffersResponse().getData(), subscriptions.getData());
    assertEquals(premiumOffer(), offer);
  }

  @Test
  void suspended_user_read_subscriptions_ok() throws ApiException {
    var suspended = anApiClient(SUSPENDED_TOKEN);
    SubscriptionApi api = new SubscriptionApi(suspended);

    GetOffersResponse subscriptions = api.getOffers();
    var offer = api.getOfferById(PREMIUM_OFFER_ID);

    assertEquals(getOffersResponse().getData(), subscriptions.getData());
    assertEquals(premiumOffer(), offer);
  }

  @Test
  void unauthenticated_user_read_subscriptions_ok() throws ApiException {
    var client = anUnauthenticatedApiClient(port);
    SubscriptionApi api = new SubscriptionApi(client);

    GetOffersResponse subscriptions = api.getOffers();
    var offer = api.getOfferById(PREMIUM_OFFER_ID);

    assertEquals(getOffersResponse().getData(), subscriptions.getData());
    assertEquals(premiumOffer(), offer);
  }
}
