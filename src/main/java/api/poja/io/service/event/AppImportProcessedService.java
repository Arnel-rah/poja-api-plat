package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.model.importer.model.FallibleResult.ofFallible;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_SUCCESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.FAILED;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;

import api.poja.io.endpoint.event.model.AppImportProcessed;
import api.poja.io.endpoint.rest.mapper.EnvironmentMapper;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.AppSetupStateService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class AppImportProcessedService implements Consumer<AppImportProcessed> {
  private final ApplicationImportService applicationImportService;
  private final ApplicationImportStateService applicationImportStateService;
  private final EnvironmentService environmentService;
  private final ApplicationService applicationService;
  private final AppSetupStateService applicationStateService;
  private final PojaConfFileMapper pojaConfFileMapper;
  private final EnvironmentMapper environmentMapper;

  @Override
  public void accept(AppImportProcessed event) {
    var orgId = event.getOrgId();
    var importId = event.getImportId();

    log.info("Setting up ApplicationImport.id={}", importId);

    var importOpt = applicationImportService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found. skip", importId);
      return;
    }
    var appImport = importOpt.get();

    var applicationOpt = applicationService.findByImportId(importId);
    if (applicationOpt.isEmpty()) {
      log.error("Application.importId={} not found. skip", importId);
      return;
    }
    var application = applicationOpt.get();
    var applicationId = application.getId();

    if (FAILED == appImport.getStatus()) {
      log.error("ApplicationImport.id={} processing has failed", importId);
      return;
    }

    // fixme: current applicationImport state can be checked prior to updating it to avoid throwing
    //      + retry if it's already in application_set_up_in_progress/final state

    applicationImportStateService.updateState(importId, APPLICATION_SET_UP_IN_PROGRESS);
    applicationStateService.save(orgId, applicationId, ENV_CREATION_IN_PROGRESS);

    log.info(
        "Configuring ApplicationImport.id={}, Application.id={} environment",
        importId,
        applicationId);
    var conf =
        new EnvConf().id(randomUUID().toString()).conf(getAppImportPojaConf(orgId, importId));
    var environmentConfigResult = ofFallible(() -> configureEnvironment(orgId, application, conf));
    if (!environmentConfigResult.isSuccess()) {
      log.error("ApplicationImport.id={} environment configuration failed", importId);
      log.error(
          environmentConfigResult.errors().stream()
              .map(Exception::getMessage)
              .collect(joining("\n\n")));
      applicationStateService.save(orgId, applicationId, ENV_CREATION_FAILED);
      applicationImportStateService.updateState(importId, APPLICATION_SET_UP_FAILED);
      return;
    }
    Environment environment = environmentConfigResult.value();

    applicationStateService.saveAll(
        orgId, applicationId, List.of(ENV_CREATION_SUCCESS, ENV_DEPLOYMENT_INITIATION_IN_PROGRESS));

    log.info("Deploying ApplicationImport.id={}, Application.id={}", importId, applicationId);
    var deployEnvResult =
        ofFallible(
            () ->
                environmentService.deployEnvWithConf(
                    orgId,
                    applicationId,
                    environment.getId(),
                    appImport.ghMainBranchName(),
                    environment.getCurrentConfId()));
    if (!deployEnvResult.isSuccess()) {
      log.info(
          "Failed to deploy ApplicationImport.id={}, Application.id={}, Environment.id={}",
          importId,
          applicationId,
          environment.getId());
      applicationStateService.save(orgId, applicationId, ENV_DEPLOYMENT_INITIATION_FAILED);
      applicationImportStateService.updateState(importId, APPLICATION_SET_UP_FAILED);
      return;
    }

    applicationStateService.save(orgId, applicationId, ENV_DEPLOYMENT_INITIATED);
    applicationImportStateService.updateState(importId, APPLICATION_SET_UP_SUCCESSFUL);
  }

  private Environment configureEnvironment(String orgId, Application application, EnvConf conf) {
    var envToConfigure = newPreprodEnvironment(application);
    return environmentService.createAndConfigureEnv(
        orgId, application, envToConfigure, conf, false /*deleteCloudPermissionsForArchived*/);
  }

  private OneOfPojaConf getAppImportPojaConf(String orgId, String importId) {
    File pojaConfFile = applicationImportService.downloadAppImportPojaConf(orgId, importId);
    return pojaConfFileMapper.readAsRest(pojaConfFile);
  }

  private Environment newPreprodEnvironment(Application application) {
    var environment =
        new CrupdateEnvironment()
            .id(randomUUID().toString())
            .archived(false)
            .environmentType(PREPROD);
    return environmentMapper.toDomain(application.getId(), environment);
  }
}
