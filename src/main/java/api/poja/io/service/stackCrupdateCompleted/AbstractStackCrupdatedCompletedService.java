package api.poja.io.service.stackCrupdateCompleted;

import api.poja.io.aws.AwsConf;
import api.poja.io.aws.iam.IamComponent;
import api.poja.io.endpoint.event.model.StackCrupdateCompleted;
import java.util.List;
import java.util.function.BiConsumer;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

public abstract class AbstractStackCrupdatedCompletedService<STACK_RESOURCE_ENTITY>
    implements BiConsumer<StackCrupdateCompleted, List<StackResource>> {
  protected final IamComponent iamComponent;
  protected final AwsConf awsConf;

  protected AbstractStackCrupdatedCompletedService(IamComponent iamComponent, AwsConf awsConf) {
    this.iamComponent = iamComponent;
    this.awsConf = awsConf;
  }

  protected abstract STACK_RESOURCE_ENTITY getStackResource(
      StackCrupdateCompleted stack, List<StackResource> stackResources);

  protected abstract IamPolicy iamPolicy(
      STACK_RESOURCE_ENTITY stackResource, List<String> usernames);
}
