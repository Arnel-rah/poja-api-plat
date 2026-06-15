package api.poja.io.service;

import static api.poja.io.endpoint.event.model.AppStatusUpdateRequested.StatusAlteration.ACTIVATE;
import static api.poja.io.endpoint.event.model.AppStatusUpdateRequested.StatusAlteration.SUSPEND;
import static api.poja.io.endpoint.rest.model.Application.StatusEnum.UNDER_MODIFICATION;
import static api.poja.io.file.ExtendedBucketComponent.getOrgBucketKey;
import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_SUCCESS;
import static java.util.function.Predicate.not;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppStatusUpdateRequested;
import api.poja.io.endpoint.event.model.ApplicationCloneRequested;
import api.poja.io.endpoint.event.model.ApplicationCreated;
import api.poja.io.endpoint.event.model.ApplicationCreationFailed;
import api.poja.io.endpoint.event.model.ApplicationCrupdated;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.rest.mapper.EnvironmentMapper;
import api.poja.io.endpoint.rest.model.Application.StatusEnum;
import api.poja.io.endpoint.rest.model.ApplicationBase;
import api.poja.io.endpoint.rest.model.CloneApplicationTemplateRequestBody;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.CreatedApplicationDTO;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.UserApplicationsDto;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.jpa.dao.ApplicationDao;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Organization;
import api.poja.io.repository.model.mapper.ApplicationMapper;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.service.validator.AppOrgValidator;
import api.poja.io.service.validator.AppValidator;
import api.poja.io.service.validator.UserAppThresholdValidator;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApplicationService {

  private final ApplicationRepository repository;
  private final ApplicationDao dao;
  private final ApplicationMapper mapper;
  private final EventProducer<PojaEvent> eventProducer;
  private final UserAppThresholdValidator appThresholdValidator;
  private final OrganizationService organizationService;
  private final EnvironmentService environmentService;
  private final AppValidator appValidator;
  private final AppOrgValidator appOrgValidator;
  private final EnvironmentMapper environmentMapper;
  private final ExtendedBucketComponent bucketComponent;
  private final EnvDeploymentConfService envDeploymentConfService;
  private final AppSetupStateService appSetupStateService;
  private final ApplicationTemplateService applicationTemplateService;

  public ApplicationService(
      ApplicationRepository repository,
      ApplicationDao dao,
      @Qualifier("DomainApplicationMapper") ApplicationMapper mapper,
      EventProducer<PojaEvent> eventProducer,
      UserAppThresholdValidator appThresholdValidator,
      OrganizationService organizationService,
      EnvironmentService environmentService,
      AppValidator appValidator,
      AppOrgValidator appOrgValidator,
      EnvironmentMapper environmentMapper,
      ExtendedBucketComponent bucketComponent,
      EnvDeploymentConfService envDeploymentConfService,
      AppSetupStateService appSetupStateService,
      ApplicationTemplateService applicationTemplateService) {
    this.repository = repository;
    this.dao = dao;
    this.mapper = mapper;
    this.eventProducer = eventProducer;
    this.appThresholdValidator = appThresholdValidator;
    this.organizationService = organizationService;
    this.environmentService = environmentService;
    this.appValidator = appValidator;
    this.appOrgValidator = appOrgValidator;
    this.environmentMapper = environmentMapper;
    this.bucketComponent = bucketComponent;
    this.envDeploymentConfService = envDeploymentConfService;
    this.appSetupStateService = appSetupStateService;
    this.applicationTemplateService = applicationTemplateService;
  }

  @Transactional
  public List<Application> saveOrgApplications(String orgId, List<ApplicationBase> toSave) {
    var org = organizationService.getById(orgId);

    var toSaveDomain = toSave.stream().map(mapper::toDomain).toList();

    validateUserApp(org, toSaveDomain);

    var saved = repository.saveAll(toSaveDomain);
    var events =
        toSaveDomain.stream().map(ApplicationService::toApplicationCrupdatedEvent).toList();
    eventProducer.accept(events);
    return saved;
  }

  public Application saveApplication(String orgId, Application toSave) {
    var org = organizationService.getById(orgId);

    validateUserApp(org, List.of(toSave));

    return save(toSave);
  }

  public Application save(Application toSave) {
    return repository.save(toSave);
  }

  private void validateUserApp(Organization org, List<Application> apps) {
    if (!apps.stream().allMatch(Application::isArchived)) {
      validateAppThreshold(org, apps);
    }
    apps.forEach(
        app -> {
          appOrgValidator.accept(app);
          appValidator.accept(app);
        });
  }

  private void validateAppThreshold(Organization org, List<Application> apps) {
    var orgOwnerId = org.getOwnerId();
    appThresholdValidator.accept(
        orgOwnerId,
        apps.stream().filter(app -> !app.isArchived()).map(Application::getId).toList());
  }

  public Application getById(String id) {
    return findById(id)
        .orElseThrow(
            () -> new NotFoundException("Application identified by id=" + id + " not found"));
  }

  public Optional<Application> findById(String id) {
    return repository.findById(id);
  }

  public Application getByImportId(String importId) {
    return findByImportId(importId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Application identified by importId=" + importId + " not found"));
  }

  public Optional<Application> findByImportId(String importId) {
    return repository.findByImportId(importId);
  }

  public Page<Application> findAllByCriteria(
      String orgId, String name, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    var data =
        dao.findAllByCriteria(
            orgId, name, PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue()));
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  public List<Application> findAllByOrgIdAndArchived(String orgId, boolean archived) {
    return repository.findAllByOrgIdAndArchived(orgId, archived);
  }

  public UserApplicationsDto findAllByOrgsOwnedByUser(
      String userId, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());
    var data = repository.findAllFromOrgsOwnedByUserByCriteria(userId, false, pageable);
    return new UserApplicationsDto(
        new Page<>(pageFromOne, boundedPageSize, data.getContent()),
        userId,
        data.getTotalElements());
  }

  public Application getByRepositoryIdAndArchived(String repositoryId, boolean archived) {
    return findByRepositoryIdAndArchived(repositoryId, archived)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Application with github_repository_id=" + repositoryId + " not found"));
  }

  public Optional<Application> findByRepositoryIdAndArchived(
      String repositoryId, boolean archived) {
    return repository.findByGithubRepositoryIdAndArchived(repositoryId, archived);
  }

  public Application getById(String id, String orgId) {
    return repository
        .findByIdAndOrgId(id, orgId)
        .orElseThrow(
            () -> new NotFoundException("Application identified by id=" + id + " not found"));
  }

  public List<Application> findAllToBillByUserId(String userId, YearMonth yearMonth) {
    return repository.findAllToBillByUserId(
        userId, yearMonth.getYear() * 100L + yearMonth.getMonth().getValue());
  }

  public List<Application> findAllToComputeBillingForByOrgId(
      String orgId, Instant computeDatetime, LocalDate endDate) {
    return repository.findAllToComputeBillingForByOrgId(orgId, computeDatetime, endDate);
  }

  public List<Application> findAllNotArchivedAndNotSuspendedByUserId(String userId) {
    return repository.findAllByUserIdAndArchived(userId, false).stream()
        .filter(not(Application::isSuspended))
        .toList();
  }

  public List<Application> findAllNotArchivedAndSuspendedByUserId(String userId) {
    return repository.findAllByUserIdAndArchived(userId, false).stream()
        .filter(Application::isSuspended)
        .toList();
  }

  public List<Application> findAllNotArchivedByUserId(String userId) {
    return repository.findAllByUserIdAndArchived(userId, false);
  }

  @Transactional
  public Application updateAppStatusAsync(
      String orgId, String applicationId, StatusEnum statusEnum) {
    var application = getById(applicationId, orgId);

    if (application.isArchived()) {
      throw new BadRequestException("Application.Id=" + application.getId() + " is archived.");
    }
    if (statusEnum.equals(application.getStatus())) {
      return application;
    }
    if (UNDER_MODIFICATION.equals(application.getStatus())) {
      throw new BadRequestException(
          "Application.Id=" + application.getId() + " status is still under modification.");
    }

    environmentService.updateUnarchivedStatusByApplicationId(
        application.getId(), Environment.StatusEnum.UNDER_MODIFICATION);

    eventProducer.accept(
        List.of(
            AppStatusUpdateRequested.builder()
                .userId(application.getUserId())
                .status(getStatus(statusEnum))
                .build()));
    return application;
  }

  private static AppStatusUpdateRequested.StatusAlteration getStatus(StatusEnum status) {
    return switch (status) {
      case ACTIVE -> ACTIVATE;
      case SUSPENDED -> SUSPEND;
      case UNKNOWN, UNDER_MODIFICATION -> throw new IllegalArgumentException();
    };
  }

  @Transactional
  public CreatedApplicationDTO createAndDeployApp(
      String orgId,
      ApplicationBase appToCreate,
      CrupdateEnvironment envToCreate,
      EnvConf envConfToCreate) {
    var environment = environmentMapper.toDomain(appToCreate.getId(), envToCreate);
    var appSaved = saveApplication(orgId, mapper.toDomain(appToCreate));
    try {
      var envSaved = createAppEnv(orgId, appSaved, environment, envConfToCreate);
      eventProducer.accept(List.of(toApplicationCreatedEvent(appSaved)));
      return new CreatedApplicationDTO(appSaved, envSaved);
    } catch (Exception e) {
      // note(try-catch): do not catch Exception superclass, catch specific exception instead!
      log.error(
          "Failed to create and deploy Application.id={} send an ApplicationCreationFailedEvent",
          appSaved.getId());
      eventProducer.accept(List.of(toApplicationCreationFailedEvent(orgId, appToCreate)));
      if (e instanceof ApiException) {
        throw e;
      }
      throw new ApiException(SERVER_EXCEPTION, "Failed to set up the application");
    }
  }

  public CreatedApplicationDTO cloneTemplate(
      String orgId, String templateId, CloneApplicationTemplateRequestBody req) {
    var template = applicationTemplateService.getById(templateId);
    var app = req.getApplication();
    var envConf = req.getEnvConf();

    var domainEnv = environmentMapper.toDomain(app.getId(), req.getEnvironment());
    var domainApp = mapper.toDomain(orgId, template, app);

    var savedApp = saveApplication(orgId, domainApp);
    try {
      var savedEnv = createAppEnv(orgId, savedApp, domainEnv, envConf);
      eventProducer.accept(
          List.of(toApplicationCloneRequested(orgId, savedApp.getId(), templateId)));
      return new CreatedApplicationDTO(savedApp, savedEnv);
    } catch (Exception e) {
      log.info("Failed to set up Application.id={} environment", savedApp.getId());
      if (e instanceof ApiException) {
        throw e;
      }
      throw new ApiException(SERVER_EXCEPTION, "Failed to set up the application");
    }
  }

  public api.poja.io.repository.model.Environment createAppEnv(
      String orgId,
      Application appSaved,
      api.poja.io.repository.model.Environment environment,
      EnvConf envConf)
      throws IllegalStateTransitionException {
    appSetupStateService.save(orgId, appSaved.getId(), ENV_CREATION_IN_PROGRESS);
    try {
      var deleteCloudPermissionsForArchived = false;
      var created =
          environmentService.createAndConfigureEnv(
              orgId, appSaved, environment, envConf, deleteCloudPermissionsForArchived);
      appSetupStateService.save(orgId, appSaved.getId(), ENV_CREATION_SUCCESS);
      return created;
    } catch (Exception e) {
      appSetupStateService.save(orgId, appSaved.getId(), ENV_CREATION_FAILED);
      throw e;
    }
  }

  public void deleteAppEnvDeplByAppId(String orgId, String appId) {
    var env = environmentService.getByApplicationId(appId);
    var confId = envDeploymentConfService.getByAppEnvDeplId(env.getCurrentConfId()).getId();
    deletePojaConfFile(orgId, appId, confId);
  }

  private void deletePojaConfFile(String orgId, String appId, String confId) {
    try {
      var bucketKey = getOrgBucketKey(orgId, appId, confId);
      bucketComponent.deleteFile(bucketKey);
      log.info("Deleted UploadedVersionedConf for appId={}", appId);
    } catch (Exception e) {
      log.error("Failed to delete UploadedVersionedConf for appId={}: {}", appId, e.getMessage());
    }
  }

  private static ApplicationCreationFailed toApplicationCreationFailedEvent(
      String orgId, ApplicationBase app) {
    return ApplicationCreationFailed.builder().orgId(orgId).appId(app.getId()).build();
  }

  private static PojaEvent toApplicationCreatedEvent(Application application) {
    return ApplicationCreated.builder()
        .orgId(application.getOrgId())
        .appId(application.getId())
        .appRepoName(application.getGithubRepositoryName())
        .installationId(application.getInstallationId())
        .description(application.getDescription())
        .repoPrivate(application.isGithubRepositoryPrivate())
        .importId(application.getImportId())
        .build();
  }

  private static PojaEvent toApplicationCrupdatedEvent(Application entity) {
    return ApplicationCrupdated.builder()
        .applicationId(entity.getId())
        .applicationRepoName(entity.getGithubRepositoryName())
        .repoUrl(entity.getGithubRepositoryUrl())
        .installationId(entity.getInstallationId())
        .description(entity.getDescription())
        .repoPrivate(entity.isGithubRepositoryPrivate())
        .previousApplicationRepoName(entity.getPreviousGithubRepositoryName())
        .archived(entity.isArchived())
        .importId(entity.getImportId())
        .build();
  }

  private static ApplicationCloneRequested toApplicationCloneRequested(
      String orgId, String appId, String templateId) {
    return ApplicationCloneRequested.builder()
        .orgId(orgId)
        .appId(appId)
        .templateId(templateId)
        .build();
  }
}
