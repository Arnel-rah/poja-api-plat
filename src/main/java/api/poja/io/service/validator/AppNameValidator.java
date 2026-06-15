package api.poja.io.service.validator;

import static com.google.common.base.Strings.isNullOrEmpty;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.jpa.ApplicationRepository;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppNameValidator implements BiConsumer<String, String> {

  public static final int DOMAIN_APP_NAME_LEN = 11;
  public static final Pattern DOMAIN_APP_NAME_PATTERN = appNamePatternWithLen(DOMAIN_APP_NAME_LEN);

  private final ApplicationRepository applicationRepository;

  // TODO: _SHOULD_ be scoped by orgId instead
  @Override
  public void accept(@Nullable String appName, String userId) {
    checkFormat(appName);
    checkNoDuplicateByUser(appName, userId);
  }

  public void checkFormat(@Nullable String appName) {
    if (isNullOrEmpty(appName) || !matchesAppNamePattern(appName, DOMAIN_APP_NAME_PATTERN)) {
      throw new BadRequestException(
          "app_name must not have more than "
              + DOMAIN_APP_NAME_LEN
              + " characters and contain only lowercase letters,"
              + " numbers and hyphen (-).");
    }
  }

  public void checkNoDuplicateByUser(String appName, String userId) {
    if (applicationRepository.existsByNameAndUserIdAndArchived(appName, userId, false)) {
      throw new BadRequestException("Application with name " + appName + " already exists.");
    }
  }

  public static boolean matchesAppNamePattern(String appName, Pattern regexPattern) {
    return regexPattern.matcher(appName).matches();
  }

  public static Pattern appNamePatternWithLen(int maxLen) {
    String regex = String.format("^[a-z0-9-]{1,%d}$", maxLen);
    return Pattern.compile(regex);
  }
}
