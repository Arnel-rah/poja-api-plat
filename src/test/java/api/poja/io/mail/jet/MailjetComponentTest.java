package api.poja.io.mail.jet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.model.exception.ApiException;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.SendContact;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class MailjetComponentTest {

  static final String SENDER_EMAIL = "contact@poja.io";
  static final String RECIPIENT_EMAIL = "recipient@gmail.com";
  static final long ACCOUNT_CREATED_TEMPLATE_ID = 1L;
  public static final String DUMMY_API_KEY = "dummy.apiKey";
  public static final String DUMMY_SECRET = "dummy.secret";

  private final MailjetComponent failingSubject = new MailjetComponent(aFailingMailjetConf());
  private final MailjetComponent subject = new MailjetComponent(aMailjetConf());

  @SneakyThrows
  @Test
  void template_with_vars_canBe_sent() {
    assertDoesNotThrow(
        () ->
            subject.sendWithTemplateTo(
                ACCOUNT_CREATED_TEMPLATE_ID, new SendContact(RECIPIENT_EMAIL)));
    var client = subject.conf().client();

    verify(client, times(1)).post(any(MailjetRequest.class));
  }

  @SneakyThrows
  @Test
  void should_throw_apiException_when_mailjet_client_fails() {
    assertThrows(
        ApiException.class,
        () ->
            failingSubject.sendWithTemplateTo(
                ACCOUNT_CREATED_TEMPLATE_ID, new SendContact(RECIPIENT_EMAIL)));

    var client = failingSubject.conf().client();
    verify(client, times(1)).post(any(MailjetRequest.class));
  }

  @SneakyThrows
  static MailjetConf aFailingMailjetConf() {
    MailjetClient client = mock();
    doThrow(MailjetException.class).when(client).post(any(MailjetRequest.class));
    var mailjetConfSpy =
        spy(
            new MailjetConf(
                SENDER_EMAIL, DUMMY_API_KEY, DUMMY_SECRET, ACCOUNT_CREATED_TEMPLATE_ID));
    when(mailjetConfSpy.client()).thenReturn(client);
    return mailjetConfSpy;
  }

  @SneakyThrows
  static MailjetConf aMailjetConf() {
    MailjetClient client = mock();
    doReturn(new MailjetResponse(201, "{}")).when(client).post(any(MailjetRequest.class));
    var mailjetConfSpy =
        spy(
            new MailjetConf(
                SENDER_EMAIL, DUMMY_API_KEY, DUMMY_SECRET, ACCOUNT_CREATED_TEMPLATE_ID));
    when(mailjetConfSpy.client()).thenReturn(client);
    return mailjetConfSpy;
  }
}
