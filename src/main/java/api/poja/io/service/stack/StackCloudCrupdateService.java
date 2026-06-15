package api.poja.io.service.stack;

import static api.poja.io.endpoint.rest.model.StackType.COMPUTE;
import static api.poja.io.endpoint.rest.model.StackType.EVENT_SCHEDULER;
import static java.util.Optional.empty;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppEnvDeployRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackCrupdateRequested;
import api.poja.io.endpoint.rest.model.StackDeployment;
import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Environment;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvDeploymentConfService;
import api.poja.io.service.EnvironmentService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StackCloudCrupdateService {
  private final ApplicationService applicationService;
  private final EnvironmentService environmentService;
  private final StackService stackService;
  private final EnvDeploymentConfService envDeploymentConfService;
  private final EventProducer<PojaEvent> eventProducer;

  /**
   * method to deploy independant stacks except EVENT_SCHEDULER which depends on event.
   *
   * @param independantStackToDeploy
   * @param orgId
   * @param applicationId
   * @param environmentId
   * @param appEnvDeployRequested
   * @return list of poja events polling stack deployment progress
   */
  public List<PojaEvent> initIndependantStackDeployment(
      StackDeployment independantStackToDeploy,
      String orgId,
      String applicationId,
      String environmentId,
      AppEnvDeployRequested appEnvDeployRequested) {
    Application app = applicationService.getById(applicationId);
    Environment env = environmentService.getById(environmentId);
    String environmentType = env.getFormattedEnvironmentType();
    Optional<Stack> optionalIndependantStack =
        stackService.findLatestByCriteria(
            applicationId, environmentId, independantStackToDeploy.getStackType(), false);
    var appEnvDeplId = appEnvDeployRequested.getAppEnvDeploymentId();
    var envDeploymentConf = envDeploymentConfService.getByAppEnvDeplId(appEnvDeplId);

    var dependantStack =
        saveDependantStackIfHasDependantStack(independantStackToDeploy, app, env, appEnvDeplId);
    if (optionalIndependantStack.isPresent()) {
      return updateStackInDbAndCreateAsyncEvent(
          independantStackToDeploy,
          orgId,
          applicationId,
          environmentId,
          appEnvDeployRequested,
          optionalIndependantStack.get(),
          appEnvDeplId,
          envDeploymentConf,
          dependantStack);
    } else {
      return createStackInDbAndCreateAsyncEvent(
          independantStackToDeploy,
          orgId,
          applicationId,
          environmentId,
          appEnvDeployRequested,
          environmentType,
          app,
          appEnvDeplId,
          dependantStack,
          envDeploymentConf);
    }
  }

  private List<PojaEvent> createStackInDbAndCreateAsyncEvent(
      StackDeployment independantStackToDeploy,
      String orgId,
      String applicationId,
      String environmentId,
      AppEnvDeployRequested appEnvDeployRequested,
      String environmentType,
      Application app,
      String appEnvDeplId,
      Optional<StackCrupdateRequested.StackPair> dependantStack,
      EnvDeploymentConf envDeploymentConf) {
    String stackName = getStackName(independantStackToDeploy, environmentType, app);
    Stack saved =
        stackService.save(
            Stack.builder()
                .name(stackName)
                .cfStackId(null)
                .applicationId(applicationId)
                .environmentId(environmentId)
                .type(independantStackToDeploy.getStackType())
                .appEnvDeplId(appEnvDeplId)
                .build());
    return List.of(
        StackCrupdateRequested.builder()
            .independantStackToDeploy(independantStackToDeploy)
            .orgId(orgId)
            .applicationId(applicationId)
            .environmentId(environmentId)
            .appEnvDeployRequested(appEnvDeployRequested)
            .appEnvDeplId(appEnvDeplId)
            .stackToCrupdate(new StackCrupdateRequested.StackPair(null, saved))
            .envDeploymentConf(envDeploymentConf)
            .dependantStack(dependantStack.orElse(null))
            .build());
  }

  private List<PojaEvent> updateStackInDbAndCreateAsyncEvent(
      StackDeployment independantStackToDeploy,
      String orgId,
      String applicationId,
      String environmentId,
      AppEnvDeployRequested appEnvDeployRequested,
      Stack independantStackToUpdate,
      String appEnvDeplId,
      EnvDeploymentConf envDeploymentConf,
      Optional<StackCrupdateRequested.StackPair> dependantStack) {
    Stack saved =
        stackService.save(
            Stack.builder()
                .name(independantStackToUpdate.getName())
                .applicationId(applicationId)
                .environmentId(environmentId)
                .type(independantStackToUpdate.getType())
                .appEnvDeplId(appEnvDeplId)
                .build());
    return List.of(
        StackCrupdateRequested.builder()
            .independantStackToDeploy(independantStackToDeploy)
            .orgId(orgId)
            .applicationId(applicationId)
            .environmentId(environmentId)
            .appEnvDeployRequested(appEnvDeployRequested)
            .appEnvDeplId(appEnvDeplId)
            .envDeploymentConf(envDeploymentConf)
            .stackToCrupdate(new StackCrupdateRequested.StackPair(independantStackToUpdate, saved))
            .dependantStack(dependantStack.orElse(null))
            .envDeploymentConf(envDeploymentConf)
            .build());
  }

  public void processIndependantStacksDeployment(
      List<StackDeployment> independantStacksDeployments,
      String orgId,
      String applicationId,
      String environmentId,
      AppEnvDeployRequested appEnvDeployRequested) {
    List<PojaEvent> eventsToSend =
        independantStacksDeployments.stream()
            .map(
                stack ->
                    this.initIndependantStackDeployment(
                        stack, orgId, applicationId, environmentId, appEnvDeployRequested))
            .flatMap(List::stream)
            .toList();
    eventProducer.accept(eventsToSend);
  }

  public String getStackName(
      StackDeployment independantStackToDeploy, String environmentType, Application app) {
    return getStackName(independantStackToDeploy.getStackType(), environmentType, app);
  }

  public String getStackName(StackType stackType, String environmentType, Application app) {
    var type = String.valueOf(stackType).toLowerCase().replace("_", "-");
    var appName = app.getFormattedName().toLowerCase();

    var formattedStackName =
        String.format("%s-%s-%s-%s", environmentType, type, appName, app.getFormattedUserId());
    if (stackService.existsByNameAndUserIdAndArchived(formattedStackName, app.getUserId(), false)) {
      return formattedStackName;
    }

    var unformattedStackName = String.format("%s-%s-%s", environmentType, type, appName);

    if (stackService.existsByNameAndUserIdAndArchived(
        unformattedStackName, app.getUserId(), false)) {
      return unformattedStackName;
    }
    return formattedStackName;
  }

  public List<String> getIndependentStacksNames(String environmentType, Application app) {
    return Stream.of(StackType.values())
        .filter(t -> t != EVENT_SCHEDULER && t != COMPUTE)
        .map(t -> getStackName(t, environmentType, app))
        .toList();
  }

  private Optional<StackCrupdateRequested.StackPair> saveDependantStackIfHasDependantStack(
      StackDeployment independantStackToDeploy,
      Application app,
      Environment env,
      String appEnvDeplId) {
    if (independantStackToDeploy.getDependantStackType() == null) {
      return empty();
    }
    StackType dependantStackType = independantStackToDeploy.getDependantStackType();
    String applicationId = app.getId();
    String environmentId = env.getId();
    var dependantStack =
        stackService.findLatestByCriteria(applicationId, environmentId, dependantStackType, false);

    if (dependantStack.isPresent()) {
      var stackToUpdate = dependantStack.get();
      return Optional.of(
          new StackCrupdateRequested.StackPair(
              dependantStack.get(),
              stackService.save(
                  Stack.builder()
                      .name(stackToUpdate.getName())
                      .cfStackId(null)
                      .applicationId(applicationId)
                      .environmentId(environmentId)
                      .type(dependantStackType)
                      .appEnvDeplId(appEnvDeplId)
                      .build())));
    }
    String stackName =
        String.format(
            "%s-%s-%s",
            env.getFormattedEnvironmentType(),
            String.valueOf(independantStackToDeploy.getDependantStackType())
                .toLowerCase()
                .replace("_", "-"),
            app.getFormattedName());
    return Optional.of(
        new StackCrupdateRequested.StackPair(
            null,
            stackService.save(
                Stack.builder()
                    .name(stackName)
                    .cfStackId(null)
                    .applicationId(applicationId)
                    .environmentId(environmentId)
                    .type(dependantStackType)
                    .appEnvDeplId(appEnvDeplId)
                    .build())));
  }
}
