package api.poja.io.mail.jet;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.transactional.SendContact;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Accessors(fluent = true)
@Configuration
public class MailjetConf {

  private final SendContact sender;

  private final Long accountCreatedTemplateId;

  @Getter(AccessLevel.NONE)
  private final ClientOptions clientOptions;

  public MailjetConf(
      @Value("${mailjet.api.sender.email}") String senderEmail,
      @Value("${mailjet.api.key}") String apiKey,
      @Value("${mailjet.api.secret}") String apiSecret,
      @Value("${mailjet.templateid.accountcreated}") Long accountCreatedTemplateId) {
    this.sender = new SendContact(senderEmail, "Poja");
    this.clientOptions = ClientOptions.builder().apiKey(apiKey).apiSecretKey(apiSecret).build();
    this.accountCreatedTemplateId = accountCreatedTemplateId;
  }

  @Bean
  public MailjetClient client() {
    return new MailjetClient(clientOptions);
  }
}
