package api.poja.io.aws.lambda;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;

@AllArgsConstructor
@Component
public class LambdaComponent {
  private final LambdaClient client;

  public void disableFunction(String functionName) {
    client.putFunctionConcurrency(
        req -> req.functionName(functionName).reservedConcurrentExecutions(0));
  }

  public void enableFunction(String functionName, Integer reservedConcurrency) {
    client.putFunctionConcurrency(
        req -> req.functionName(functionName).reservedConcurrentExecutions(reservedConcurrency));
  }

  public Integer getFunctionReservedConcurrency(String functionName) {
    return client
        .getFunctionConcurrency(req -> req.functionName(functionName))
        .reservedConcurrentExecutions();
  }
}
