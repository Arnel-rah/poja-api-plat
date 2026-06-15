package api.poja.io.endpoint.rest.mapper;

import api.poja.io.aws.iam.model.ConsoleUserCredentials;
import api.poja.io.endpoint.rest.model.ConsoleUser;
import org.springframework.stereotype.Component;

@Component
public class ConsoleUserMapper {
  public ConsoleUser toRest(String orgId, ConsoleUserCredentials credentials) {
    return new ConsoleUser()
        .orgId(orgId)
        .accountId(credentials.accountId())
        .consoleLoginUrl(credentials.accountConsoleSigninUri())
        .password(credentials.password())
        .username(credentials.username());
  }
}
