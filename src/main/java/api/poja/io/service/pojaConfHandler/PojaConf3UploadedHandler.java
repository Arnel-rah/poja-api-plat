package api.poja.io.service.pojaConfHandler;

import static api.poja.io.model.PojaVersion.POJA_3;
import static api.poja.io.service.git.GitUtils.gitRm;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileFinder;
import api.poja.io.file.FileUnzipper;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.exception.InternalServerErrorException;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf3.PojaConf3;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.AppEnvironmentDeploymentService;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvDeploymentConfService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.api.pojaSam.PojaSamApi;
import api.poja.io.service.appEnvConfigurer.AppEnvConfigurerService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.workflows.DeploymentStateService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.springframework.stereotype.Component;

@Component
@Slf4j
final class PojaConf3UploadedHandler extends AbstractPojaConfUploadedHandler {
  private static final String CD_COMPUTE_DIRECTORY = "poja-templates/cd-compute.yml";

  public PojaConf3UploadedHandler(
      PojaDeploymentCloudEnv pojaDeploymentCloudEnv,
      AppEnvConfigurerService appEnvConfigurerService,
      FileUnzipper unzipper,
      DeploymentStateService deploymentStateService,
      ApplicationService appService,
      EnvironmentService envService,
      AppInstallationService appInstallService,
      ExtendedBucketComponent bucketComponent,
      PojaSamApi pojaSamApi,
      GithubService githubService,
      EnvDeploymentConfService envDeploymentConfService,
      AppEnvironmentDeploymentService appEnvironmentDeploymentService,
      FileFinder finder,
      PojaConfFileMapper pojaConfFileMapper) {
    super(
        pojaDeploymentCloudEnv,
        appEnvConfigurerService,
        unzipper,
        deploymentStateService,
        appService,
        envService,
        appInstallService,
        bucketComponent,
        pojaSamApi,
        githubService,
        envDeploymentConfService,
        appEnvironmentDeploymentService,
        finder,
        pojaConfFileMapper);
  }

  @Override
  protected PojaVersion supportedPojaVersion() {
    return POJA_3;
  }

  @Override
  protected void onAfterClone(
      Git git,
      CredentialsProvider ghCredentialsProvider,
      Application app,
      Environment env,
      File generatedCode,
      Path cloneDirPath,
      String appEnvDeploymentId,
      PojaConf pojaConf) {
    super.onAfterClone(
        git,
        ghCredentialsProvider,
        app,
        env,
        generatedCode,
        cloneDirPath,
        appEnvDeploymentId,
        pojaConf);
    rmUnusedPreviouslyGeneratedFiles(git, cloneDirPath, pojaConf);
  }

  @Override
  public void configureCdCompute(Path clonedDirPath, PojaConf pojaConf) {
    File rawCdComputeFile = bucketComponent.download(CD_COMPUTE_DIRECTORY);
    var ghWorkflowDir = Path.of(clonedDirPath + "/.github/workflows/cd-compute.yml");
    try {
      var rawFilePath = Paths.get(rawCdComputeFile.toURI());
      var fileContent = new String(Files.readAllBytes(rawFilePath));
      var replacedContent = fileContent.replace(ENV_PLACEHODLER, pojaDeploymentCloudEnv.env());
      Files.write(rawFilePath, replacedContent.getBytes(), TRUNCATE_EXISTING);
      Files.copy(rawFilePath, ghWorkflowDir, REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("Error occurred during configuration of cd-compute-file.");
      throw new InternalServerErrorException(e);
    }
  }

  @SneakyThrows
  private void rmUnusedPreviouslyGeneratedFiles(Git git, Path cloneDirPath, PojaConf pojaConf) {
    var domain = (PojaConf3) pojaConf;
    rmIntegrationGeneratedFiles(git, cloneDirPath, domain.integration());
    List<PojaConf2.ScheduledTask> scheduledTasks = domain.scheduledTasks();
    if (scheduledTasks.isEmpty()) {
      gitRm(git, List.of(Path.of(".github/workflows/cd-scheduler.yml")));
    }
    Path packageSourcePath = getPackageSourcePath(cloneDirPath, domain.general().packageFullName());
    Path handlerPath = packageSourcePath.resolve("handler");
    gitRm(git, List.of(handlerPath.resolve("model"), handlerPath.resolve("exceptionHandler")));
    var previousHandlers =
        finder.apply(packageSourcePath, Set.of("MailboxEventHandler.java", "ApiEventHandler.java"));
    var handlers =
        previousHandlers.stream()
            .filter(f -> !f.getParent().getFileName().toString().equals("handler"))
            .toList();
    if (!handlers.isEmpty()) {
      gitRm(git, handlers.stream().map(cloneDirPath::relativize).toList());
    }
  }
}
