package api.poja.io.aws.cloudformation;

import static api.poja.io.model.CancelMethod.CANCEL_UPDATE_STACK;
import static api.poja.io.model.CancelMethod.DELETE_STACK;
import static api.poja.io.model.UpdateStackResult.UpdateStatus.NO_UPDATE_NEEDED;
import static api.poja.io.model.UpdateStackResult.UpdateStatus.UPDATE_SUCCESS;
import static java.util.Optional.empty;
import static software.amazon.awssdk.services.cloudformation.model.Capability.CAPABILITY_NAMED_IAM;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_IN_PROGRESS;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.REVIEW_IN_PROGRESS;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_IN_PROGRESS;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;

import api.poja.io.model.CancelMethod;
import api.poja.io.model.StackStatus;
import api.poja.io.model.UpdateStackResult;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.InternalServerErrorException;
import api.poja.io.model.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

@Slf4j
@Component
@AllArgsConstructor
public class CloudformationComponent {
  private final CloudFormationClient cloudFormationClient;

  private static List<Tag> setUpTags(Map<String, String> tags) {
    return tags.entrySet().stream()
        .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
        .toList();
  }

  private static List<Parameter> setUpParameters(Map<String, String> parameters) {
    return parameters.entrySet().stream()
        .map(
            param ->
                Parameter.builder()
                    .parameterKey(param.getKey())
                    .parameterValue(param.getValue())
                    .build())
        .toList();
  }

  public Optional<StackStatus> getStackStatus(String stackId) {
    Optional<Stack> stack = findStackByName(stackId);
    if (stack.isEmpty()) {
      return empty();
    }
    var status = stack.get().stackStatus();
    var isResumable =
        CREATE_COMPLETE.equals(status)
            || UPDATE_COMPLETE.equals(status)
            || UPDATE_ROLLBACK_COMPLETE.equals(status);
    var isCancellable =
        UPDATE_IN_PROGRESS.equals(status)
            || CREATE_IN_PROGRESS.equals(status)
            || REVIEW_IN_PROGRESS.equals(status);
    var cancelMethod = getCancelMethod(status);
    return Optional.of(new StackStatus(isCancellable, isResumable, cancelMethod));
  }

  public String createStack(
      String stackName,
      String templateUrl,
      Map<String, String> parameters,
      Map<String, String> tags) {
    List<Parameter> stackParameters = setUpParameters(parameters);
    List<Tag> stackTags = setUpTags(tags);

    log.info("createStack with tags {}", stackTags);

    CreateStackRequest request =
        CreateStackRequest.builder()
            .stackName(stackName)
            .templateURL(templateUrl)
            .parameters(stackParameters)
            .tags(stackTags)
            .capabilities(CAPABILITY_NAMED_IAM)
            .build();
    try {
      return cloudFormationClient.createStack(request).stackId();
    } catch (CloudFormationException e) {
      throw new BadRequestException(
          String.format(
              "An error occurred during stack(%s) creation: %s", stackName, e.getMessage()));
    }
  }

  public UpdateStackResult updateStack(
      String stackName,
      String templateUrl,
      Map<String, String> parameters,
      Map<String, String> tags) {
    List<Parameter> stackParameters = setUpParameters(parameters);
    List<Tag> stackTags = setUpTags(tags);

    UpdateStackRequest request =
        UpdateStackRequest.builder()
            .parameters(stackParameters)
            .templateURL(templateUrl)
            .stackName(stackName)
            .tags(stackTags)
            .capabilities(CAPABILITY_NAMED_IAM)
            .build();

    try {
      var stackId = cloudFormationClient.updateStack(request).stackId();
      return new UpdateStackResult(UPDATE_SUCCESS, stackId);
    } catch (CloudFormationException e) {
      if (e.getMessage().contains("No updates are to be performed")) {
        return new UpdateStackResult(NO_UPDATE_NEEDED, null);
      }
      throw new InternalServerErrorException(
          String.format(
              "An error occurred during stack(%s) update: %s", stackName, e.getMessage()));
    }
  }

  public Optional<Stack> findStackByName(String stackName) {
    DescribeStacksRequest request = DescribeStacksRequest.builder().stackName(stackName).build();

    try {
      DescribeStacksResponse response = cloudFormationClient.describeStacks(request);
      if (!response.hasStacks()) {
        return empty();
      }
      return Optional.of(response.stacks().getFirst());
    } catch (AwsServiceException | SdkClientException e) {
      if (e.getMessage().contains("Stack with id " + stackName + " does not exist")) {
        return empty();
      }
      throw new InternalServerErrorException(e);
    }
  }

  public Optional<String> findStackIdByName(String stackName) {
    return this.findStackByName(stackName).map(Stack::stackId);
  }

  public List<StackEvent> getStackEvents(String stackIdOrName) {
    DescribeStackEventsRequest request =
        DescribeStackEventsRequest.builder().stackName(stackIdOrName).build();
    try {
      return cloudFormationClient.describeStackEvents(request).stackEvents();
    } catch (CloudFormationException e) {
      throw new InternalServerErrorException(
          String.format(
              "An error occurred when retrieving stack(%s) events: %s",
              stackIdOrName, e.getMessage()));
    }
  }

  public List<Output> getStackOutputs(String stackName) {
    Optional<Stack> optionalStack = this.findStackByName(stackName);
    if (optionalStack.isEmpty()) {
      throw new NotFoundException("Stack(" + stackName + ") not found");
    }
    return optionalStack.get().outputs();
  }

  public void deleteStack(String stackName) {
    this.findStackByName(stackName);
    DeleteStackRequest request = DeleteStackRequest.builder().stackName(stackName).build();
    try {
      cloudFormationClient.deleteStack(request);
    } catch (AwsServiceException | SdkClientException e) {
      if (e.getMessage().contains("Stack with id " + stackName + " does not exist")) {
        throw new NotFoundException("Stack(" + stackName + ") does not exist");
      }
      throw new InternalServerErrorException(e);
    }
  }

  public List<StackResource> getStackResources(String stackIdOrName) {
    DescribeStackResourcesRequest request =
        DescribeStackResourcesRequest.builder().stackName(stackIdOrName).build();
    try {
      DescribeStackResourcesResponse response =
          cloudFormationClient.describeStackResources(request);
      if (!response.hasStackResources()) {
        throw new NotFoundException("Stack(" + stackIdOrName + ") does not exist");
      }
      return response.stackResources();
    } catch (AwsServiceException | SdkClientException e) {
      throw new RuntimeException(e);
    }
  }

  public void cancelExistingStackUpdate(String stackId) {
    cloudFormationClient.cancelUpdateStack(req -> req.stackName(stackId));
  }

  private CancelMethod getCancelMethod(
      software.amazon.awssdk.services.cloudformation.model.StackStatus stackStatus) {
    if (UPDATE_IN_PROGRESS.equals(stackStatus)) {
      return CANCEL_UPDATE_STACK;
    }
    if (CREATE_IN_PROGRESS.equals(stackStatus)) {
      return DELETE_STACK;
    }
    return null;
  }
}
