package api.poja.io.service.validator;

import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_COMPLETED;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.model.Application;
import api.poja.io.service.OrganizationSetupStateService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class AppOrgValidator implements Consumer<Application> {
  private final OrganizationSetupStateService organizationSetupStateService;

  @Override
  public void accept(Application application) {
    var orgId = application.getOrgId();
    var latestOrgState =
        organizationSetupStateService
            .getLatestStateByOrgId(orgId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Organization with id = " + orgId + " has no latest state"));

    if (!ORGANIZATION_SETUP_COMPLETED.equals(latestOrgState.getProgressionStatus())) {
      throw new BadRequestException(
          "Organization with id = "
              + orgId
              + " is in state "
              + latestOrgState.getProgressionStatus());
    }
  }
}
