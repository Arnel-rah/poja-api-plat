package api.poja.io.mail.jet;

import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import api.poja.io.model.exception.ApiException;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.transactional.SendContact;
import com.mailjet.client.transactional.SendEmailsRequest;
import com.mailjet.client.transactional.TransactionalEmail;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MailjetComponent {

  @Getter
  @Accessors(fluent = true)
  private final MailjetConf conf;

  public void sendWithTemplateTo(long templateId, SendContact to, Map<String, Object> variables) {
    var transactionalEmail =
        TransactionalEmail.builder()
            .from(conf.sender())
            .to(to)
            .templateID(templateId)
            .variables(variables)
            .build();

    var req = SendEmailsRequest.builder().message(transactionalEmail).build();

    try {
      req.sendWith(conf.client());
    } catch (MailjetException e) {
      throw new ApiException(SERVER_EXCEPTION, "Failed to send mailjet transactional email: " + e);
    }
  }

  public void sendWithTemplateTo(long templateId, SendContact to) {
    sendWithTemplateTo(templateId, to, Map.of());
  }
}
