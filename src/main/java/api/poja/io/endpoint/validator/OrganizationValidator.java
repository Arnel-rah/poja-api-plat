package api.poja.io.endpoint.validator;

import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.model.exception.BadRequestException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OrganizationValidator implements Consumer<Organization> {
  private static final Pattern organizationNameRegexPattern =
      Pattern.compile("^[\\w+=,.@-]{1,128}$");

  @Override
  public void accept(Organization organization) {
    var exceptionMessageBuilder = new StringBuilder();

    if (!isAValidOrgName(organization.getName())) {
      exceptionMessageBuilder.append(
          "Organization.name must be 1–128 characters long and contain only letters, numbers, and"
              + " the following characters: = , . @ - _ +");
    }

    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private boolean isAValidOrgName(String name) {
    return organizationNameRegexPattern.matcher(name).matches();
  }
}
