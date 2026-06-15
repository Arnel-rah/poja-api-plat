package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_INITIATED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_QUEUED;

import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.EnvironmentDeploymentRepository;
import api.poja.io.repository.jpa.dao.EnvironmentDeploymentDao;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.service.appEnvConfigurer.AppEnvConfigurerService;
import api.poja.io.service.github.model.GhGetCommitResponse;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppEnvironmentDeploymentService {
  private static final List<DeploymentStateEnum> DEPLOYMENT_STILL_IN_PROGRESS_STATES =
      List.of(
          INDEPENDENT_STACKS_DEPLOYMENT_INITIATED,
          INDEPENDENT_STACKS_DEPLOYMENT_QUEUED,
          INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS,
          INDEPENDENT_STACKS_DEPLOYED,
          COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS);

  private final EnvironmentDeploymentRepository repository;
  private final EnvironmentDeploymentDao dao;
  private final EnvDeploymentConfService envDeploymentConfService;
  private final AppEnvConfigurerService appEnvConfigurerService;
  private final AppInstallationService appInstallationService;
  private final GithubComponent githubComponent;

  public AppEnvironmentDeployment save(
      AppEnvironmentDeployment actual, String installationId, EnvDeploymentConf envDeploymentConf) {
    var installation = appInstallationService.getById(installationId);
    GhGetCommitResponse commitInfo =
        githubComponent.getCommitInfo(
            actual.getGhRepoOwnerName(),
            installation.getGhId(),
            actual.getGhRepoName(),
            actual.getGhCommitSha());
    GhGetCommitResponse.GhUser committer = commitInfo.committer();
    return save(
        actual.toBuilder()
            .deployedUrl(null)
            .envDeplConfId(envDeploymentConf.getId())
            .ghCommitUrl(commitInfo.commit().url().toString())
            .ghCommitterAvatarUrl(committer == null ? null : committer.avatarUrl().toString())
            .ghCommitterEmail(commitInfo.commit().committer().email())
            .ghCommitterId(committer == null ? null : committer.id())
            .ghCommitterLogin(committer == null ? null : committer.login())
            .ghCommitterName(commitInfo.commit().committer().name())
            .ghCommitterType(committer == null ? null : committer.type())
            .build());
  }

  public AppEnvironmentDeployment save(AppEnvironmentDeployment appEnvironmentDeployment) {
    return repository.save(appEnvironmentDeployment);
  }

  @Transactional
  public void updateWorkflowRunId(String id, String workflowRunId, String workflowRunAttempt) {
    repository.updateWorkflowRunId(id, workflowRunId, workflowRunAttempt);
  }

  public void updateEnvDeploymentConf(String id, String envDeploymentConfId) {
    repository.updateEnvDeploymentConfId(id, envDeploymentConfId);
  }

  public void updateGhTagInfo(String id, String tagName, String tagMessage) {
    repository.updateGhTagInfo(id, tagName, tagMessage);
  }

  @Transactional
  public void updateDeployedUri(String id, URI uri) {
    repository.updateDeployedUri(id, uri.toString());
  }

  public AppEnvironmentDeployment getByAppIdAndId(String appId, String id) {
    return findByAppIdAndId(appId, id)
        .orElseThrow(
            () -> new NotFoundException("AppEnvironmentDeployment.Id = " + id + " not found."));
  }

  public AppEnvironmentDeployment getById(String id) {
    return findById(id)
        .orElseThrow(
            () -> new NotFoundException("AppEnvironmentDeployment.Id = " + id + " not found."));
  }

  public AppEnvironmentDeployment getByIdAndEnvType(String id, EnvironmentType environmentType) {
    return findByIdAndEnvType(id, environmentType)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "AppEnvironmentDeployment.Id = "
                        + id
                        + " and envType = "
                        + environmentType
                        + " not found."));
  }

  public Optional<AppEnvironmentDeployment> findByIdAndEnvType(
      String id, EnvironmentType environmentType) {
    return repository.findByIdAndEnv_EnvironmentType(id, environmentType);
  }

  public Optional<AppEnvironmentDeployment> findById(String id) {
    return repository.findById(id);
  }

  public Optional<AppEnvironmentDeployment> findByAppIdAndId(String appId, String id) {
    return repository.findByAppIdAndId(appId, id);
  }

  public Page<AppEnvironmentDeployment> findAllByCriteria(
      String orgId,
      String appId,
      EnvironmentType envType,
      Instant startDatetime,
      Instant endDatetime,
      PageFromOne page,
      BoundedPageSize pageSize) {
    var data =
        dao.findAllByCriteria(
            appId,
            envType,
            startDatetime,
            endDatetime,
            PageRequest.of(page.getValue() - 1, pageSize.getValue()));
    return new Page<>(page, pageSize, data);
  }

  public Page<AppEnvironmentDeployment> findAllWithDeployedComputeStack(
      String userId, String appId, String envId, PageFromOne page, BoundedPageSize pageSize) {
    var data = findAllByCriteria(appId, envId, List.of(COMPUTE_STACK_DEPLOYED), page, pageSize);
    return new Page<>(page, pageSize, data);
  }

  public List<AppEnvironmentDeployment> findAllByCriteria(
      String appId,
      String envId,
      List<DeploymentStateEnum> deploymentStateEnums,
      PageFromOne page,
      BoundedPageSize pageSize) {
    return dao.findAllByCriteria(
        appId,
        envId,
        deploymentStateEnums,
        PageRequest.of(page.getValue() - 1, pageSize.getValue()));
  }

  public List<AppEnvironmentDeployment> findAllByCriteria(
      String appId, String envId, List<DeploymentStateEnum> deploymentStateEnums) {
    return dao.findAllByCriteria(appId, envId, deploymentStateEnums);
  }

  @Transactional
  public OneOfPojaConf getConfig(String orgId, String appId, String deploymentId) {
    var persisted = getById(deploymentId);
    var deploymentConf = envDeploymentConfService.getById(persisted.getEnvDeplConfId());
    String pojaConfFileKey = deploymentConf.getPojaConfFileKey();
    return appEnvConfigurerService.readConfig(
        orgId, appId, persisted.getEnv().getId(), pojaConfFileKey);
  }

  public List<AppEnvironmentDeployment> findAllPendingDeployments(String appId, String envId) {
    return findAllByCriteria(appId, envId, DEPLOYMENT_STILL_IN_PROGRESS_STATES);
  }
}
