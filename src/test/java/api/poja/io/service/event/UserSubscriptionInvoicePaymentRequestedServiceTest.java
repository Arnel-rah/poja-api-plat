package api.poja.io.service.event;

import api.poja.io.conf.FacadeIT;
import api.poja.io.endpoint.event.model.UserSubscriptionInvoicePaymentRequested;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("run in local only")
class UserSubscriptionInvoicePaymentRequestedServiceTest extends FacadeIT {
  /* insert in data
    insert into invoice(id, amount, invoice_id, status, invoice_url, user_id)
  VALUES ('invoice1_id', 1, null, 'UNKNOWN', null, 'joe-doe-id');
  insert into user_subscription(id, user_id, offer_id, subscription_begin_datetime, subscription_end_datetime, invoice_id)
  VALUES ('sub1_id', 'joe-doe-id', 'offer1_id', '2024-03-25T12:00:00.00Z', null, 'invoice1_id');
     */
  @Autowired UserSubscriptionInvoicePaymentRequestedService subject;

  @Test
  void accept() {
    String invoice1Id = "invoice1_id";
    subject.accept(
        UserSubscriptionInvoicePaymentRequested.builder()
            .userSubscriptionId("sub1_id")
            .userId("joe-doe-id")
            .invoiceId(invoice1Id)
            .build());
  }
}
