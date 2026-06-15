package api.poja.io.service.stackCrupdateCompleted;

import static api.poja.io.aws.AwsConf.iamUsernameArn;
import static api.poja.io.repository.model.EventStackResource.computeQueueArn;
import static api.poja.io.repository.model.EventStackResource.getSqsQueueUri;
import static api.poja.io.service.stack.StackCloudService.getPhysicalResourceId;
import static com.amazonaws.services.lambda.runtime.events.IamPolicyResponse.ALLOW;
import static software.amazon.awssdk.policybuilder.iam.IamPrincipalType.AWS;

import api.poja.io.aws.AwsConf;
import api.poja.io.aws.iam.IamComponent;
import api.poja.io.aws.sqs.SqsComponent;
import api.poja.io.endpoint.event.model.StackCrupdateCompleted;
import api.poja.io.repository.model.EventStackResource;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.EventStackResourceService;
import api.poja.io.service.organization.OrganizationService;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Service
@Slf4j
public class EventStackCrupdateCompletedService
    extends AbstractStackCrupdatedCompletedService<EventStackResource> {
  public static final String EVENT_STACK_USED_IAM_POLICY_VERSION = "2008-10-17";
  private final OrganizationService organizationService;
  private final EventStackResourceService eventStackResourceService;
  private final SqsComponent sqsComponent;

  public EventStackCrupdateCompletedService(
      IamComponent iamComponent,
      AwsConf awsConf,
      OrganizationService organizationService,
      EventStackResourceService eventStackResourceService,
      SqsComponent sqsComponent) {
    super(iamComponent, awsConf);
    this.organizationService = organizationService;
    this.eventStackResourceService = eventStackResourceService;
    this.sqsComponent = sqsComponent;
  }

  @Override
  public void accept(
      StackCrupdateCompleted eventStackCrupdateCompleted, List<StackResource> stackResources) {
    if (stackResources.isEmpty()) {
      return;
    }
    var org = organizationService.getById(eventStackCrupdateCompleted.getOrgId());
    var usernames = List.of(org.getConsoleUsername());
    EventStackResource resource = getStackResource(eventStackCrupdateCompleted, stackResources);
    if (!existsIgnoringIdAndCreationTimestamp(resource)) {
      var saved = eventStackResourceService.save(resource);
      String region = awsConf.getRegion().toString();
      String accountId = awsConf.getAccountId();
      for (var mailboxQueueName : saved.getMailboxQueueNames()) {
        updateMailboxQueuePolicy(mailboxQueueName, region, accountId, usernames);
      }
      for (var deadLetterQueueName : saved.getDeadLetterQueueNames()) {
        updateDeadLetterQueuePolicy(deadLetterQueueName, region, accountId, usernames);
      }
    }
  }

  private boolean existsIgnoringIdAndCreationTimestamp(EventStackResource resource) {
    List<EventStackResource> existingEntities =
        eventStackResourceService.findAllByEnvironmentId(resource.getEnvId());
    return existingEntities.stream()
        .map(EventStackCrupdateCompletedService::resetGeneratedValues)
        .toList()
        .contains(resetGeneratedValues(resource));
  }

  private static EventStackResource resetGeneratedValues(EventStackResource resource) {
    return resource.toBuilder()
        .id(null)
        .creationTimestamp(null)
        /*
            do not include as generated values in order to have compute stack resources per deployment
        .id(null)
        */
        .build();
  }

  private void updateMailboxQueuePolicy(
      String queueName, String region, String accountId, List<String> usernames) {
    if (queueName == null) {
      return;
    }
    URI queueUri = getSqsQueueUri(region, awsConf.getAccountId(), queueName);
    sqsComponent.updateSqsQueuePolicy(
        queueUri.toString(),
        mailboxQueuePolicy(computeQueueArn(region, accountId, queueName), usernames));
  }

  private void updateDeadLetterQueuePolicy(
      String queueName, String region, String accountId, List<String> usernames) {
    sqsComponent.updateSqsQueuePolicy(
        getSqsQueueUri(region, awsConf.getAccountId(), queueName).toString(),
        deadLetterQueuePolicy(computeQueueArn(region, accountId, queueName), usernames));
  }

  @Override
  protected final EventStackResource getStackResource(
      StackCrupdateCompleted stackCrupdateCompleted, List<StackResource> stackResources) {
    List<String> mailboxQueueNames = new ArrayList<>();
    List<String> deadLetterQueueNames = new ArrayList<>();

    for (int i = 1; i <= 10; i++) {
      var mailboxPhysicalId = getPhysicalResourceId(stackResources, "MailboxQueue" + i);
      var dlPhysicalId = getPhysicalResourceId(stackResources, "DeadLetterQueue" + i);
      if (mailboxPhysicalId == null && dlPhysicalId == null) {
        break;
      }
      if (mailboxPhysicalId != null) {
        mailboxQueueNames.add(extractQueueName(mailboxPhysicalId));
      }
      if (dlPhysicalId != null) {
        deadLetterQueueNames.add(extractQueueName(dlPhysicalId));
      }
    }

    Stack crupdatedStack = stackCrupdateCompleted.getCrupdatedStack();
    return EventStackResource.builder()
        .stackId(crupdatedStack.getId())
        .creationTimestamp(stackCrupdateCompleted.getCompletionTimestamp())
        .mailboxQueueNames(mailboxQueueNames)
        .deadLetterQueueNames(deadLetterQueueNames)
        .appEnvDeplId(stackCrupdateCompleted.getAppEnvDeplId())
        .envId(crupdatedStack.getEnvironmentId())
        // .eventStackPolicyDocumentName(crupdatedStack.getName() + "-policies")
        .build();
  }

  private static String extractQueueName(String queuePhysicalId) {
    if (queuePhysicalId == null) {
      return null;
    }
    return queuePhysicalId.substring(queuePhysicalId.lastIndexOf("/") + 1);
  }

  @Override
  protected final IamPolicy iamPolicy(
      EventStackResource eventStackResource, List<String> usernames) {
    // unsupported because here we will attach policies to the resource, not attach all resources to
    // an user
    throw new UnsupportedOperationException();
  }

  private IamPolicy mailboxQueuePolicy(String mailboxQueueArn, List<String> usernames) {
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
        .version(EVENT_STACK_USED_IAM_POLICY_VERSION)
        .addStatement(
            req ->
                req.effect(ALLOW)
                    .principals(principals)
                    .addAction("sqs:GetQueueAttributes")
                    .addAction("sqs:GetQueueUrl")
                    .addAction("sqs:ReceiveMessage")
                    .addAction("sqs:ChangeMessageVisibility")
                    .addAction("sqs:DeleteMessage")
                    .addAction("sqs:PurgeQueue")
                    .addAction("sqs:SendMessage")
                    .resourceIds(List.of(mailboxQueueArn)))
        .build();
  }

  private IamPolicy deadLetterQueuePolicy(String deadLetterQueueArn, List<String> usernames) {
    List<IamPrincipal> principals =
        usernames.stream()
            .map(
                username ->
                    IamPrincipal.builder()
                        .type(AWS)
                        .id(iamUsernameArn(awsConf.getAccountId(), username))
                        .build())
            .toList();
    return mailboxQueuePolicy(deadLetterQueueArn, usernames).toBuilder()
        .addStatement(
            req ->
                req.effect(ALLOW)
                    .principals(principals)
                    .addAction("sqs:ListMessageMoveTasks")
                    .addAction("sqs:StartMessageMoveTask")
                    .addAction("sqs:CancelMessageMoveTask")
                    .addAction("sqs:ListDeadLetterSourceQueues")
                    .resourceIds(List.of(deadLetterQueueArn)))
        .build();
  }
}
