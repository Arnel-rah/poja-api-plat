package api.poja.io.service.validator;

import api.poja.io.model.exception.BadRequestException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EmailValidator implements Predicate<String>, Consumer<String> {
  private static final Pattern EMAIL_REGEX_PATTERN =
      Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

  @Override
  public void accept(String email) {
    if (!test(email)) {
      throw new BadRequestException("Email is not a valid email address.");
    }
  }

  @Override
  public boolean test(String email) {
    return email != null && EMAIL_REGEX_PATTERN.matcher(email).matches();
  }
}
