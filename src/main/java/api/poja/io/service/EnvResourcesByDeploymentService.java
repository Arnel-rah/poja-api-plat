package api.poja.io.service;

import api.poja.io.aws.AwsConf;
import api.poja.io.endpoint.rest.model.ComputeStackResource;
import api.poja.io.endpoint.rest.model.EnvironmentResourceByDeployment;
import api.poja.io.endpoint.rest.model.EventStackResource;
import api.poja.io.endpoint.rest.model.StorageBucketStackResource;
import api.poja.io.endpoint.rest.model.WorkerFunction;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.page.Page;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EnvResourcesByDeploymentService {
  private final AppEnvironmentDeploymentService deploymentService;
  private final ComputeStackResourceService computeStackResourceService;
  private final EventStackResourceService eventStackResourceService;
  private final StorageBucketStackResourceService storageBucketStackResourceService;
  private final AwsConf awsConf;

  @Transactional
  public Page<EnvironmentResourceByDeployment> getEnvironmentResourcesByDeploymentByCriteria(
      String orgId, String appId, String envId, PageFromOne page, BoundedPageSize pageSize) {
    var deployments =
        deploymentService.findAllWithDeployedComputeStack(orgId, appId, envId, page, pageSize);
    var data =
        deployments.data().stream()
            .map(d -> getEnvironmentResourceByDeploymentByCriteria(orgId, appId, envId, d))
            .toList();
    return new Page<>(page, pageSize, data);
  }

  private EnvironmentResourceByDeployment getEnvironmentResourceByDeploymentByCriteria(
      String orgId, String appId, String envId, AppEnvironmentDeployment appEnvDepl) {
    String appEnvDeplId = appEnvDepl.getId();

    return new EnvironmentResourceByDeployment()
        .applicationEnvironmentDeploymentId(appEnvDeplId)
        .deploymentDatetime(appEnvDepl.getCreationDatetime())
        .environmentType(appEnvDepl.getEnv().getEnvironmentType())
        .commitMessage(appEnvDepl.getGhCommitMessage())
        .commitSha(appEnvDepl.getGhCommitSha())
        .environmentId(envId)
        .computeStackResource(computeStackResource(appEnvDepl))
        .eventStackResource(eventStackResource(appEnvDepl))
        .storageBucketStackResource(storageBucketStackResource(appEnvDepl));
  }

  private ComputeStackResource computeStackResource(AppEnvironmentDeployment appEnvDepl) {
    var optionalByAppEnvDeplId =
        computeStackResourceService.findOneByAppEnvDeplId(appEnvDepl.getId());
    if (optionalByAppEnvDeplId.isEmpty()) {
      return null;
    }
    var stackResource = optionalByAppEnvDeplId.get();
    return new ComputeStackResource()
        .id(stackResource.getId())
        .functionDashboardUrlPrefix(computeStackResourceService.getFunctionDashboardUrlPrefix())
        .frontalFunctionName(stackResource.getFrontalFunctionName())
        .workerFunctions(
            stackResource.getWorkerFunctions().stream()
                .map(w -> new WorkerFunction().name(w.getName()))
                .toList())
        .creationDatetime(stackResource.getCreationDatetime());
  }

  private StorageBucketStackResource storageBucketStackResource(
      AppEnvironmentDeployment appEnvDepl) {
    var optionalStackResource =
        storageBucketStackResourceService.findOneByAppEnvDeplId(appEnvDepl.getId());
    if (optionalStackResource.isEmpty()) {
      return null;
    }
    var stackResource = optionalStackResource.get();
    return new StorageBucketStackResource()
        .id(stackResource.getId())
        .bucketName(stackResource.getBucketName())
        .bucketUri(stackResource.getUri(awsConf.getRegion().toString()));
  }

  private EventStackResource eventStackResource(AppEnvironmentDeployment appEnvDepl) {
    var optionalEventStackResource =
        eventStackResourceService.findOneByAppEnvDeplId(appEnvDepl.getId());
    if (optionalEventStackResource.isEmpty()) {
      return null;
    }
    String region = awsConf.getRegion().toString();
    String accountId = awsConf.getAccountId();
    var eventStackResource = optionalEventStackResource.get();
    return new EventStackResource()
        .id(eventStackResource.getId())
        .queues(eventStackResource.queueResourceGroups(region, accountId));
  }
}
