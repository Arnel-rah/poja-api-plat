package api.poja.io.service.stackCrupdateCompleted;

import static api.poja.io.aws.AwsConf.iamUsernameArn;
import static api.poja.io.service.stack.StackCloudService.getPhysicalResourceId;
import static com.amazonaws.services.lambda.runtime.events.IamPolicyResponse.ALLOW;
import static software.amazon.awssdk.policybuilder.iam.IamPrincipalType.AWS;

import api.poja.io.aws.AwsConf;
import api.poja.io.aws.iam.IamComponent;
import api.poja.io.endpoint.event.model.StackCrupdateCompleted;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.repository.model.Stack;
import api.poja.io.repository.model.StorageBucketStackResource;
import api.poja.io.service.StorageBucketStackResourceService;
import api.poja.io.service.organization.OrganizationService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Service
@Slf4j
public class StorageBucketStackCrupdateCompletedService
    extends AbstractStackCrupdatedCompletedService<StorageBucketStackResource> {
  private final OrganizationService organizationService;
  private final StorageBucketStackResourceService storageBucketStackResourceService;
  private final ExtendedBucketComponent extendedBucketComponent;

  public StorageBucketStackCrupdateCompletedService(
      IamComponent iamComponent,
      AwsConf awsConf,
      OrganizationService organizationService,
      StorageBucketStackResourceService storageBucketStackResourceService,
      ExtendedBucketComponent extendedBucketComponent) {
    super(iamComponent, awsConf);
    this.organizationService = organizationService;
    this.storageBucketStackResourceService = storageBucketStackResourceService;
    this.extendedBucketComponent = extendedBucketComponent;
  }

  @Override
  public void accept(
      StackCrupdateCompleted bucketStackCrupdateCompleted, List<StackResource> stackResources) {
    if (stackResources.isEmpty()) {
      return;
    }
    var org = organizationService.getById(bucketStackCrupdateCompleted.getOrgId());
    StorageBucketStackResource stackResource =
        getStackResource(bucketStackCrupdateCompleted, stackResources);
    if (!existsIgnoringIdAndCreationTimestamp(stackResource)) {
      var saved = storageBucketStackResourceService.save(stackResource);
      extendedBucketComponent.changeBucketPolicy(
          saved.getBucketName(), iamPolicy(saved, List.of(org.getConsoleUsername())));
    }
  }

  @Override
  protected final StorageBucketStackResource getStackResource(
      StackCrupdateCompleted stackCrupdateCompleted, List<StackResource> stackResources) {
    String BUCKET_LOGICAL_RESOURCE_ID = "Bucket";
    var bucketName = getPhysicalResourceId(stackResources, BUCKET_LOGICAL_RESOURCE_ID);
    Stack crupdatedStack = stackCrupdateCompleted.getCrupdatedStack();
    return StorageBucketStackResource.builder()
        .stackId(crupdatedStack.getId())
        .bucketName(bucketName)
        .appEnvDeplId(stackCrupdateCompleted.getAppEnvDeplId())
        .envId(crupdatedStack.getEnvironmentId())
        .creationTimestamp(stackCrupdateCompleted.getCompletionTimestamp())
        .build();
  }

  private boolean existsIgnoringIdAndCreationTimestamp(StorageBucketStackResource stackResource) {
    List<StorageBucketStackResource> existingEntities =
        storageBucketStackResourceService.findAllByEnvId(stackResource.getEnvId());
    return existingEntities.stream()
        .map(StorageBucketStackCrupdateCompletedService::resetGeneratedValues)
        .toList()
        .contains(resetGeneratedValues(stackResource));
  }

  private static StorageBucketStackResource resetGeneratedValues(
      StorageBucketStackResource resource) {
    return resource.toBuilder()
        .id(null)
        /*
            do not include as generated values in order to have compute stack resources per deployment
        .id(null)
        */
        .creationTimestamp(null)
        .build();
  }

  @Override
  protected final IamPolicy iamPolicy(
      StorageBucketStackResource storageBucketStackResource, List<String> usernames) {
    List<IamPrincipal> principals =
        usernames.stream()
            .map(
                username ->
                    IamPrincipal.builder()
                        .type(AWS)
                        .id(iamUsernameArn(awsConf.getAccountId(), username))
                        .build())
            .toList();
    return IamPolicy.builder()
        .addStatement(
            req ->
                req.effect(ALLOW)
                    .principals(principals)
                    .addAction("s3:GetObject")
                    .addAction("s3:GetObjectAttributes")
                    .addAction("s3:GetObjectRetention")
                    .addAction("s3:GetObjectTorrent")
                    .addAction("s3:GetObjectVersion")
                    .addAction("s3:GetObjectVersionAttributes")
                    .addAction("s3:GetObjectVersionForReplication")
                    .addAction("s3:GetObjectVersionTorrent")
                    .addAction("s3:InitiateReplication")
                    .addAction("s3:ListMultipartUploadParts")
                    .addAction("s3:PutObject")
                    .addAction("s3:PutObjectRetention")
                    .addAction("s3:DeleteObject")
                    .addAction("s3:DeleteObjectVersion")
                    .resourceIds(List.of(storageBucketStackResource.getArnWithAllObjects()))
                    .build())
        .addStatement(
            req ->
                req.effect(ALLOW)
                    .principals(principals)
                    .addAction("s3:ListBucket")
                    .addAction("s3:GetBucketVersioning")
                    .addAction("s3:PutBucketVersioning")
                    .resourceIds(List.of(storageBucketStackResource.getArn())))
        .build();
  }
}
