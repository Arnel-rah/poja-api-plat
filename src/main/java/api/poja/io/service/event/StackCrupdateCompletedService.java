package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.StackCrupdateCompleted;
import api.poja.io.service.stack.StackCloudService;
import api.poja.io.service.stackCrupdateCompleted.ComputeStackCrupdateCompletedService;
import api.poja.io.service.stackCrupdateCompleted.EventStackCrupdateCompletedService;
import api.poja.io.service.stackCrupdateCompleted.StorageBucketStackCrupdateCompletedService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class StackCrupdateCompletedService implements Consumer<StackCrupdateCompleted> {
  private final ComputeStackCrupdateCompletedService computeStackCrupdateCompletedService;
  private final StorageBucketStackCrupdateCompletedService
      storageBucketStackCrupdateCompletedService;
  private final EventStackCrupdateCompletedService eventStackCrupdateCompletedService;
  private final StackCloudService stackCloudService;

  @Override
  public void accept(StackCrupdateCompleted stackCrupdateCompleted) {
    var stack = stackCrupdateCompleted.getCrupdatedStack();
    var stackName = stack.getName();
    switch (stack.getType()) {
      case COMPUTE_PERMISSION -> {
        log.info("not handled stack. do nothing");
      }
      case EVENT -> {
        eventStackCrupdateCompletedService.accept(
            stackCrupdateCompleted, stackCloudService.getStackResources(stackName));
      }
      case STORAGE_BUCKET -> {
        storageBucketStackCrupdateCompletedService.accept(
            stackCrupdateCompleted, stackCloudService.getStackResources(stackName));
      }
      case COMPUTE -> {
        computeStackCrupdateCompletedService.accept(
            stackCrupdateCompleted, stackCloudService.getStackResources(stackName));
      }
    }
  }
}
