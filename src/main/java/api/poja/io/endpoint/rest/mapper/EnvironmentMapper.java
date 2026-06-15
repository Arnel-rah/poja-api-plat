package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.Environment.StateEnum.UNKNOWN;
import static java.lang.Boolean.TRUE;

import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import java.net.URI;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("RestEnvironmentMapper")
public class EnvironmentMapper {
  public Environment toRest(api.poja.io.repository.model.Environment domain) {
    Optional<AppEnvironmentDeployment> latestDeployment = domain.getLatestDeployment();
    String activeConfId =
        latestDeployment.map(AppEnvironmentDeployment::getEnvDeplConfId).orElse(null);
    return new Environment()
        .id(domain.getId())
        .status(domain.getStatus())
        .state(domain.getState())
        .activeDeploymentUri(latestDeployment.map(d -> URI.create(d.getDeployedUrl())).orElse(null))
        .activeConfId(activeConfId)
        .currentConfId(Optional.ofNullable(domain.getCurrentConfId()).orElse(activeConfId))
        .appliedConfId(Optional.ofNullable(domain.getAppliedConfId()).orElse(activeConfId))
        .environmentType(domain.getEnvironmentType());
  }

  public api.poja.io.repository.model.Environment toDomain(
      String applicationId, CrupdateEnvironment rest) {
    return api.poja.io.repository.model.Environment.builder()
        .id(rest.getId())
        .archived(TRUE.equals(rest.getArchived()))
        .applicationId(applicationId)
        .state(UNKNOWN)
        .environmentType(rest.getEnvironmentType())
        .build();
  }
}
