package api.poja.io.repository.model.mapper;

import static api.poja.io.file.ApplicationImportFileType.ENV_VARS_FILE;
import static api.poja.io.file.ApplicationImportFileType.ZIPPED_CODE;
import static api.poja.io.file.ExtendedBucketComponent.ENV_VARS_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.INITIAL_ZIPPED_CODE_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.getAppImportBucketKey;
import static api.poja.io.file.TempFile.createTempDir;
import static api.poja.io.model.PojaVersion.LATEST;
import static java.lang.Boolean.TRUE;

import api.poja.io.endpoint.rest.mapper.EnvVarMapper;
import api.poja.io.endpoint.rest.model.CreateApplicationImportRequestBody;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileUnzipper;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.service.organization.OrganizationService;
import java.io.File;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component("applicationImportDomainMapper")
@AllArgsConstructor
public class ApplicationImportMapper {
  private final OrganizationService organizationService;
  private final ExtendedBucketComponent bucketComponent;
  private final FileUnzipper unzipper;
  private final EnvVarMapper envVarMapper;

  public ApplicationImport toDomain(CreateApplicationImportRequestBody rest, String orgId) {
    var org = organizationService.getById(orgId);
    var githubRepository = rest.getGithubRepository();
    // assert githubRepository != null
    return ApplicationImport.builder()
        .id(rest.getId())
        .appName(rest.getName())
        .pojaVersion(LATEST.toHumanReadableValue())
        .githubRepositoryHttpUrl(githubRepository.getHtmlUrl().toString())
        .githubRepositoryName(githubRepository.getName())
        .githubRepositoryDefaultBranch(githubRepository.getDefaultBranch())
        .githubRepositoryPrivate(TRUE.equals(githubRepository.getIsPrivate()))
        .githubRepositoryDescription(githubRepository.getDescription())
        .githubRepositoryId(githubRepository.getId())
        .appInstallationId(githubRepository.getInstallationId())
        .orgId(org.getId())
        .userId(rest.getUserId())
        .build();
  }

  public UnknownApplication toUnknownApplication(ApplicationImport appImport) {
    var codeBucketKey =
        getAppImportBucketKey(
            appImport.getOrgId(), appImport.getId(), ZIPPED_CODE, INITIAL_ZIPPED_CODE_FILENAME);
    var envVarsBucketKey =
        getAppImportBucketKey(
            appImport.getOrgId(), appImport.getId(), ENV_VARS_FILE, ENV_VARS_FILENAME);
    var zippedCode = bucketComponent.download(codeBucketKey);
    var envVarsFile = bucketComponent.download(envVarsBucketKey);

    var unzippedCode = createTempDir("unzipped-code");
    unzip(asZipFile(zippedCode), unzippedCode);

    return new UnknownApplication(unzippedCode.toFile(), envVarMapper.toDomain(envVarsFile));
  }

  public UnknownApplication toUnknownApplication(
      ApplicationImport appImport, String snapshotFilename) {
    var codeBucketKey =
        getAppImportBucketKey(
            appImport.getOrgId(), appImport.getId(), ZIPPED_CODE, snapshotFilename);
    var envVarsBucketKey =
        getAppImportBucketKey(
            appImport.getOrgId(), appImport.getId(), ENV_VARS_FILE, ENV_VARS_FILENAME);
    var zippedCode = bucketComponent.download(codeBucketKey);
    var envVarsFile = bucketComponent.download(envVarsBucketKey);

    var unzippedCode = createTempDir("unzipped-code");
    unzip(asZipFile(zippedCode), unzippedCode);

    return new UnknownApplication(unzippedCode.toFile(), envVarMapper.toDomain(envVarsFile));
  }

  @SneakyThrows
  private static ZipFile asZipFile(File toUnzip) {
    return new ZipFile(toUnzip);
  }

  private void unzip(ZipFile downloaded, Path destination) {
    unzipper.apply(downloaded, destination);
  }
}
