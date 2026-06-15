package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.StackCrupdateRequested.*;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackCrupdateRequested;
import api.poja.io.model.tag.PojaTags;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.stack.StackCloudService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StackCrupdateRequestedService implements Consumer<StackCrupdateRequested> {
  private final StackCloudService stackCloudService;
  private final EventProducer<PojaEvent> eventProducer;
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;

  private static Map<String, String> getParametersFrom(String environmentType) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("Env", environmentType);
    return parameters;
  }

  @Override
  public void accept(StackCrupdateRequested stackCrupdateRequested) {
    var independantStackToDeploy = stackCrupdateRequested.getIndependantStackToDeploy();
    var applicationId = stackCrupdateRequested.getApplicationId();
    var environmentId = stackCrupdateRequested.getEnvironmentId();
    var orgId = stackCrupdateRequested.getOrgId();
    var appEnvDeployRequested = stackCrupdateRequested.getAppEnvDeployRequested();
    var envDeploymentConf = stackCrupdateRequested.getEnvDeploymentConf();
    var app = applicationService.getById(applicationId);
    var env = environmentService.getById(environmentId);
    String environmentType = env.getFormattedEnvironmentType();
    Map<String, String> parameters = getParametersFrom(environmentType);
    Map<String, String> tags =
        PojaTags.from(app.getUserId(), app.getId(), app.getName(), environmentType);
    StackPair stackPairToCrupdate = stackCrupdateRequested.getStackToCrupdate();
    Stack stackToCrupdate = stackPairToCrupdate.last();
    Optional<Stack> optionalIndependantStack = Optional.ofNullable(stackPairToCrupdate.first());

    if (optionalIndependantStack.isPresent()) {
      var events =
          stackCloudService.updateStackOrCreateIfNotExistsOnCloud(
              independantStackToDeploy,
              orgId,
              applicationId,
              environmentId,
              appEnvDeployRequested,
              parameters,
              envDeploymentConf,
              tags,
              new StackPair(optionalIndependantStack.get(), stackToCrupdate),
              Optional.ofNullable(stackCrupdateRequested.getDependantStack()));
      eventProducer.accept(events);
    } else {
      var events =
          stackCloudService.createStack(
              independantStackToDeploy,
              orgId,
              applicationId,
              environmentId,
              appEnvDeployRequested,
              parameters,
              envDeploymentConf,
              tags,
              stackToCrupdate,
              Optional.ofNullable(stackCrupdateRequested.getDependantStack()));
      eventProducer.accept(events);
    }
  }
}
