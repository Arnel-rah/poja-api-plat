package api.poja.io.service.event;

import static api.poja.io.file.ApplicationImportFileType.ENV_VARS_FILE;
import static api.poja.io.file.ApplicationImportFileType.ZIPPED_CODE;
import static api.poja.io.file.ExtendedBucketComponent.ENV_VARS_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.INITIAL_ZIPPED_CODE_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.getAppImportBucketKey;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.cloneRepository;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportUploadRequested;
import api.poja.io.endpoint.event.model.AppLanguageAnalysisRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileWriter;
import api.poja.io.file.FileZipper;
import api.poja.io.model.EnvVar;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.github.GithubService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AppImportUploadRequestedService implements Consumer<AppImportUploadRequested> {
  private final ExtendedBucketComponent bucketComponent;
  private final ObjectMapper objectMapper;
  private final FileWriter fileWriter;
  private final FileZipper fileZipper;
  private final AppInstallationService appInstallationService;
  private final GithubService githubService;
  private final EventProducer<AppLanguageAnalysisRequested> eventProducer;

  @Override
  public void accept(AppImportUploadRequested event) {
    log.info("The app import info is about to be uploaded");
    var appImport = event.getAppImport();

    var appInstallation = appInstallationService.getById(appImport.getAppInstallationId());
    var ghToken =
        githubService.getInstallationToken(appInstallation.getGhId(), event.maxConsumerDuration());
    var credentials = new UsernamePasswordCredentialsProvider("x-access-token", ghToken);
    var cloneDir = createTempDir(randomUUID().toString());

    try (var git = cloneRepository(credentials, appImport.getGithubRepositoryHttpUrl(), cloneDir)) {
      String defaultBranchName = appImport.getGithubRepositoryDefaultBranch();
      var doesBranchExist = doesBranchExist(credentials, git, defaultBranchName);
      checkoutBranch(doesBranchExist, git, defaultBranchName);
      uploadEnvVars(appImport.getOrgId(), appImport.getId(), event.getEnvVars());
      uploadZippedCode(appImport.getOrgId(), appImport.getId(), cloneDir);
      fireLangAnalysisEvent(appImport.getId(), appImport.getOrgId());
    } catch (GitAPIException e) {
      log.error("Failed to clone repository: ", e);
    } catch (Exception e) {
      log.error("Error: ", e);
    }
  }

  private void fireLangAnalysisEvent(String importId, String orgId) {
    var event = AppLanguageAnalysisRequested.builder().importId(importId).orgId(orgId).build();
    eventProducer.accept(List.of(event));
  }

  private void uploadEnvVars(String orgId, String appImportId, List<EnvVar> envVars)
      throws JsonProcessingException {
    var envVarsBytes = objectMapper.writeValueAsBytes(envVars);
    var envVarsFile = fileWriter.apply(envVarsBytes, null);
    var envVarsKey = getAppImportBucketKey(orgId, appImportId, ENV_VARS_FILE, ENV_VARS_FILENAME);
    bucketComponent.upload(envVarsFile, envVarsKey);
  }

  private void uploadZippedCode(String orgId, String appImportId, Path cloneDir) {
    var zippedCode = cloneDir.getParent().resolve(randomUUID().toString());
    fileZipper.apply(cloneDir, zippedCode);
    var zippedCodeKey =
        getAppImportBucketKey(orgId, appImportId, ZIPPED_CODE, INITIAL_ZIPPED_CODE_FILENAME);
    bucketComponent.upload(zippedCode.toFile(), zippedCodeKey);
  }

  @SneakyThrows
  private static Path createTempDir(String prefix) {
    return Files.createTempDirectory(prefix);
  }
}
