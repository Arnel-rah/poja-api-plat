package api.poja.io.aws.iam.model;

import java.net.URI;

public record ConsoleUserCredentials(String username, String password, String accountId) {
  private static final String AWS_LOGIN_URL_TEMPLATE = "https://%s.signin.aws.amazon.com/console";

  public URI accountConsoleSigninUri() {
    return URI.create(AWS_LOGIN_URL_TEMPLATE.formatted(accountId));
  }
}
