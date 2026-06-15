package api.poja.io.service;

import static api.poja.io.file.ApplicationImportFileType.ANALYSIS_RESULT;
import static api.poja.io.file.ApplicationImportFileType.GRADLE_BUILD_FILE;
import static api.poja.io.file.ApplicationImportFileType.POJA_CONF_FILE;
import static api.poja.io.file.ApplicationImportFileType.ZIPPED_CODE;
import static api.poja.io.file.ExtendedBucketComponent.APP_IMPORT_POJA_CONF_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.APP_LANG_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.BUILD_TOOL_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.INITIAL_ZIPPED_CODE_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.INITIAL_ZIPPED_GRADLE_BUILD_TOOL_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.POST_CONFLICT_RESOLUTION_GRADLE_BUILD_TOOL_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.POST_CONVERSION_ZIPPED_GRADLE_BUILD_TOOL_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.getAppImportBucketKey;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.PENDING;
import static java.io.File.createTempFile;
import static java.nio.file.Files.readAllBytes;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportUploadRequested;
import api.poja.io.endpoint.rest.mapper.EnvVarMapper;
import api.poja.io.endpoint.rest.model.CreateApplicationImportRequestBody;
import api.poja.io.file.ApplicationImportFileType;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.file.FileZipper;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.importer.analyzer.buildtool.BuildToolFile;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolData;
import api.poja.io.model.importer.analyzer.lang.AppLangAnalyzerData;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.ApplicationImportRepository;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.enums.ApplicationImportStatus;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.repository.model.mapper.ApplicationMapper;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.service.validator.ApplicationImportValidator;
import api.poja.io.service.validator.UserAppImportValidator;
import api.poja.io.service.validator.UserAppThresholdValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ApplicationImportService {
  private final ApplicationImportRepository repository;
  private final ApplicationImportMapper mapper;
  private final ApplicationImportValidator validator;
  private final EventProducer<AppImportUploadRequested> eventProducer;
  private final EnvVarMapper envVarMapper;
  private final ExtendedBucketComponent bucketComponent;
  private final FileUnzipper fileUnzipper;
  private final FileZipper fileZipper;
  private final ObjectMapper objectMapper;
  private final FileWriter fileWriter;
  private final UserAppThresholdValidator appThresholdValidator;
  private final OrganizationService organizationService;
  private final UserAppImportValidator userAppImportValidator;
  private final ApplicationService applicationService;
  private final ApplicationMapper applicationMapper;

  public ApplicationImportService(
      ApplicationImportRepository repository,
      @Qualifier("applicationImportDomainMapper") ApplicationImportMapper mapper,
      EventProducer<AppImportUploadRequested> eventProducer,
      EnvVarMapper envVarMapper,
      ExtendedBucketComponent bucketComponent,
      FileUnzipper fileUnzipper,
      FileZipper fileZipper,
      ApplicationImportValidator validator,
      ObjectMapper objectMapper,
      FileWriter fileWriter,
      UserAppThresholdValidator appThresholdValidator,
      OrganizationService organizationService,
      UserAppImportValidator userAppImportValidator,
      ApplicationService applicationService,
      ApplicationMapper applicationMapper) {
    this.repository = repository;
    this.validator = validator;
    this.mapper = mapper;
    this.eventProducer = eventProducer;
    this.envVarMapper = envVarMapper;
    this.bucketComponent = bucketComponent;
    this.fileUnzipper = fileUnzipper;
    this.fileZipper = fileZipper;
    this.objectMapper = objectMapper;
    this.fileWriter = fileWriter;
    this.appThresholdValidator = appThresholdValidator;
    this.organizationService = organizationService;
    this.userAppImportValidator = userAppImportValidator;
    this.applicationService = applicationService;
    this.applicationMapper = applicationMapper;
  }

  @Transactional
  public ApplicationImport importApplication(
      String orgId, CreateApplicationImportRequestBody createImportRequestBody) {
    var repoId = requireNonNull(createImportRequestBody.getGithubRepository()).getId();
    validator.accept(createImportRequestBody);
    userAppImportValidator.accept(repoId);

    var organization = organizationService.getById(orgId);
    var orgOwnerId = organization.getOwnerId();
    // Check if user can create ONE more app
    appThresholdValidator.accept(orgOwnerId, List.of(randomUUID().toString()));

    var appImport = mapper.toDomain(createImportRequestBody, orgId);
    appImport.setStatus(PENDING);

    var application = applicationMapper.toDomain(appImport, randomUUID().toString());
    var savedApplication = applicationService.save(application);

    appImport.setCreatedAppId(savedApplication.getId());
    repository.save(appImport);

    var envVars =
        createImportRequestBody.getEnvironmentVariables().stream()
            .map(envVarMapper::fromRest)
            .toList();
    var event = AppImportUploadRequested.builder().appImport(appImport).envVars(envVars).build();
    eventProducer.accept(List.of(event));
    return appImport;
  }

  @Transactional
  public void updateStatus(String importId, ApplicationImportStatus status) {
    repository.updateStatus(importId, status);
  }

  public ApplicationImport getById(String importId) {
    return findById(importId)
        .orElseThrow(
            () -> new NotFoundException("ApplicationImport#id=" + importId + " not found"));
  }

  public ApplicationImport getByOrgIdAndId(String orgId, String id) {
    return repository
        .findByOrgIdAndIdAndArchived(orgId, id, false)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "ApplicationImport(orgId=" + orgId + ", id=" + id + ") not found"));
  }

  public Page<ApplicationImport> findPaginatedByOrgId(
      String orgId, PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    List<ApplicationImport> data = repository.findAllByOrgIdAndArchived(orgId, pageable, false);
    return new Page<>(page, pageSize, data);
  }

  public Optional<ApplicationImport> findById(String importId) {
    return repository.findById(importId);
  }

  public File downloadAppImportFile(
      String orgId, String importId, ApplicationImportFileType fileType, String filename) {
    var bucketKey = getAppImportBucketKey(orgId, importId, fileType, filename);
    return bucketComponent.download(bucketKey);
  }

  private void downloadAndUnzipAppImportFile(
      String orgId,
      String importId,
      ApplicationImportFileType fileType,
      String filename,
      @Nonnull Path target) {
    var zippedCode = downloadAppImportFile(orgId, importId, fileType, filename);
    try {
      fileUnzipper.apply(new ZipFile(zippedCode), target);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void downloadAndUnzipCode(String orgId, String importId, @Nonnull Path target) {
    downloadAndUnzipAppImportFile(
        orgId, importId, ZIPPED_CODE, INITIAL_ZIPPED_CODE_FILENAME, target);
  }

  public void downloadAndUnzipCodeSnapshot(
      String orgId, String importId, String snapshotFilename, @Nonnull Path target) {
    downloadAndUnzipAppImportFile(orgId, importId, ZIPPED_CODE, snapshotFilename, target);
  }

  public void downloadAndUnzipGradleBuildPostConversion(
      String orgId, String importId, @Nonnull Path target) {
    downloadAndUnzipAppImportFile(
        orgId,
        importId,
        GRADLE_BUILD_FILE,
        POST_CONVERSION_ZIPPED_GRADLE_BUILD_TOOL_FILENAME,
        target);
  }

  public void downloadAndUnzipGradleBuildPostConflictResolution(
      String orgId, String importId, @Nonnull Path target) {
    downloadAndUnzipAppImportFile(
        orgId,
        importId,
        GRADLE_BUILD_FILE,
        POST_CONFLICT_RESOLUTION_GRADLE_BUILD_TOOL_FILENAME,
        target);
  }

  public void uploadZippedCodeSnapshot(
      String orgId, String importId, String snapshotFilename, Path root) {
    var zippedCode = root.getParent().resolve(randomUUID().toString());
    fileZipper.apply(root, zippedCode);
    var zippedCodeKey = getAppImportBucketKey(orgId, importId, ZIPPED_CODE, snapshotFilename);
    bucketComponent.upload(zippedCode.toFile(), zippedCodeKey);
  }

  public void uploadInitialZippedBuildToolFiles(
      String orgId, String importId, Collection<Path> paths) throws IOException {
    if (paths.isEmpty()) {
      throw new IllegalArgumentException("No paths provided");
    }

    var zipped = createTempFile(INITIAL_ZIPPED_GRADLE_BUILD_TOOL_FILENAME, null);
    fileZipper.apply(zipped.toPath(), paths);

    var initialZippedBuildToolKey =
        getAppImportBucketKey(
            orgId, importId, GRADLE_BUILD_FILE, INITIAL_ZIPPED_GRADLE_BUILD_TOOL_FILENAME);
    bucketComponent.upload(zipped, initialZippedBuildToolKey);
  }

  public void uploadPostConvZippedBuildToolFiles(
      String orgId, String importId, @Nonnull Path root) {
    List<Path> buildToolFiles =
        Arrays.stream(BuildToolFile.ALL).map(root::resolve).filter(Files::exists).toList();

    var zipped = root.getParent().resolve(randomUUID().toString());
    fileZipper.apply(zipped, buildToolFiles);

    var postConvZippedBuildToolKey =
        getAppImportBucketKey(
            orgId, importId, GRADLE_BUILD_FILE, POST_CONVERSION_ZIPPED_GRADLE_BUILD_TOOL_FILENAME);
    bucketComponent.upload(zipped.toFile(), postConvZippedBuildToolKey);
  }

  public void uploadPostConflictResolutionZippedBuildToolFiles(
      String orgId, String importId, @Nonnull Path root) {
    List<Path> buildToolFiles =
        Arrays.stream(BuildToolFile.ALL).map(root::resolve).filter(Files::exists).toList();

    var zipped = root.getParent().resolve(randomUUID().toString());
    fileZipper.apply(zipped, buildToolFiles);

    var postConvZippedBuildToolKey =
        getAppImportBucketKey(
            orgId,
            importId,
            GRADLE_BUILD_FILE,
            POST_CONFLICT_RESOLUTION_GRADLE_BUILD_TOOL_FILENAME);
    bucketComponent.upload(zipped.toFile(), postConvZippedBuildToolKey);
  }

  public File downloadAppImportPojaConf(String orgId, String importId) {
    var pojaConfBucketKey =
        getAppImportBucketKey(orgId, importId, POJA_CONF_FILE, APP_IMPORT_POJA_CONF_FILENAME);
    return bucketComponent.download(pojaConfBucketKey);
  }

  public void uploadAppImportLangAnalysisResult(
      String orgId, String importId, AppLangAnalyzerData analysisResult) throws IOException {
    var bucketKey =
        getAppImportBucketKey(orgId, importId, ANALYSIS_RESULT, APP_LANG_ANALYSIS_RESULT_FILENAME);
    var resultBytes = objectMapper.writeValueAsBytes(analysisResult);
    var resultFile = fileWriter.apply(resultBytes, null);
    bucketComponent.upload(resultFile, bucketKey);
  }

  public void uploadBuildToolAnalysisResultData(
      String orgId, String importId, BuildToolData resultData) throws IOException {
    var bucketKey =
        getAppImportBucketKey(
            orgId, importId, ANALYSIS_RESULT, BUILD_TOOL_ANALYSIS_RESULT_FILENAME);
    var resultBytes = objectMapper.writeValueAsBytes(resultData);
    var resultFile = fileWriter.apply(resultBytes, null);
    bucketComponent.upload(resultFile, bucketKey);
  }

  public BuildToolData downloadBuildToolAnalysisResultData(String orgId, String importId) {
    File file =
        downloadAppImportFile(
            orgId, importId, ANALYSIS_RESULT, BUILD_TOOL_ANALYSIS_RESULT_FILENAME);
    try {
      return objectMapper.readValue(readAllBytes(file.toPath()), BuildToolData.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ApplicationImport getByIdAndRepositoryId(String id, String repoId) {
    return findByIdAndRepositoryId(id, repoId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No application import with id=" + id + " and repoId=" + repoId + " found"));
  }

  public Optional<ApplicationImport> findByIdAndRepositoryId(String id, String repoId) {
    return repository.findByIdAndGithubRepositoryIdAndArchived(id, repoId, false);
  }
}
