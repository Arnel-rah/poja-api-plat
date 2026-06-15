package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.StackResourceStatusType.UNKNOWN_TO_SDK_VERSION;

import api.poja.io.endpoint.rest.model.Stack;
import api.poja.io.endpoint.rest.model.StackEvent;
import api.poja.io.endpoint.rest.model.StackOutput;
import api.poja.io.endpoint.rest.model.StackResourceStatusType;
import api.poja.io.model.exception.InternalServerErrorException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;

@Component
@AllArgsConstructor
@Slf4j
public class StackMapper {
  public Stack toRest(api.poja.io.repository.model.Stack domain) {
    return new Stack()
        .id(domain.getId())
        .name(domain.getName())
        .cfStackId(domain.getCfStackId())
        .applicationId(domain.getApplicationId())
        .environmentId(domain.getEnvironmentId())
        .stackType(domain.getType())
        .creationDatetime(domain.getCreationDatetime())
        .updateDatetime(domain.getUpdateDatetime())
        .archived(domain.isArchived());
  }

  public StackEvent toRest(software.amazon.awssdk.services.cloudformation.model.StackEvent domain) {
    return new StackEvent()
        .eventId(domain.eventId())
        .logicalResourceId(domain.logicalResourceId())
        .resourceType(domain.resourceType())
        .resourceStatus(toRestStackEventStatusType(domain.resourceStatus()))
        .timestamp(domain.timestamp())
        .statusMessage(domain.resourceStatusReason());
  }

  public StackOutput toRest(Output domain) {
    return new StackOutput()
        .key(domain.outputKey())
        .value(domain.outputValue())
        .description(domain.description());
  }

  private StackResourceStatusType toRestStackEventStatusType(ResourceStatus domain) {
    try {
      if (domain == ResourceStatus.UNKNOWN_TO_SDK_VERSION) {
        return UNKNOWN_TO_SDK_VERSION;
      }
      return StackResourceStatusType.valueOf(domain.toString());
    } catch (IllegalArgumentException e) {
      log.error("No enum constant for value: {}", domain.getClass());
      throw new InternalServerErrorException(e);
    }
  }
}
