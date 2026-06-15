package api.poja.io.service.stack;

import api.poja.io.aws.cloudformation.CloudformationComponent;
import api.poja.io.endpoint.rest.model.StackOutput;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudformation.model.Output;

@Service
@AllArgsConstructor
public class StackOutputService {
  private final CloudformationComponent cloudformationComponent;

  public List<StackOutput> getStackOutputs(String stackId) {
    return cloudformationComponent.getStackOutputs(stackId).stream()
        .map(StackOutputService::toStackOutput)
        .toList();
  }

  private static StackOutput toStackOutput(Output output) {
    return new StackOutput()
        .key(output.outputKey())
        .value(output.outputValue())
        .description(output.description());
  }
}
