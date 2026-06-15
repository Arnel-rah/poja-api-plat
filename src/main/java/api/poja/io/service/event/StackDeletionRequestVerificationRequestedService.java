package api.poja.io.service.event;

import static software.amazon.awssdk.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.DELETE_FAILED;

import api.poja.io.aws.cloudformation.CloudformationComponent;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackDeletionRequestVerificationRequested;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.stack.StackService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

@Service
@AllArgsConstructor
@Slf4j
public class StackDeletionRequestVerificationRequestedService
    implements Consumer<StackDeletionRequestVerificationRequested> {
  private final StackService stackService;
  private final CloudformationComponent cloudformationComponent;
  private final EventProducer<PojaEvent> eventProducer;

  @Override
  public void accept(
      StackDeletionRequestVerificationRequested stackDeletionRequestVerificationRequested) {
    Stack toDelete = stackDeletionRequestVerificationRequested.getToDelete();
    var optionalStack = cloudformationComponent.findStackByName(toDelete.getCfStackId());
    if (optionalStack.isPresent()) {
      StackStatus stackStatus = optionalStack.get().stackStatus();
      if (DELETE_COMPLETE.equals(stackStatus)) {
        stackService.archiveStack(toDelete);
        return;
      }
      if (DELETE_FAILED.equals(stackStatus)) {
        eventProducer.accept(
            List.of(stackDeletionRequestVerificationRequested.getRetryOnFailureEvent()));
        return;
      } else {
        throw new EventConsumptionBackOffException(
            "stack "
                + toDelete.getCfStackId()
                + " should be deleted but not in DELETE_IN_PROGRESS or DELETE_COMPLETE step yet");
      }
    }
    log.error("unexpected error : stack {} is missing", toDelete.getCfStackId());
  }
}
