package api.poja.io.service.event;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackDeletionRequestVerificationRequested;
import api.poja.io.endpoint.event.model.StackDeletionRequested;
import api.poja.io.service.stack.StackCloudService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StackDeletionRequestedService implements Consumer<StackDeletionRequested> {
  private final StackCloudService stackCloudService;
  private final EventProducer<PojaEvent> eventProducer;

  @Override
  public void accept(StackDeletionRequested stackDeletionRequested) {
    stackCloudService.initiateStackDelete(stackDeletionRequested.getToDelete());
    eventProducer.accept(
        List.of(
            new StackDeletionRequestVerificationRequested(
                stackDeletionRequested.getToDelete(), stackDeletionRequested)));
  }
}
