package api.poja.io.service.event;

import api.poja.io.aws.lambda.LambdaComponent;
import api.poja.io.endpoint.event.model.LambdaFunctionStatusUpdateRequested;
import api.poja.io.sys.platform.SaasOnly;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@SaasOnly
public class LambdaFunctionStatusUpdateRequestedService
    implements Consumer<LambdaFunctionStatusUpdateRequested> {
  private final LambdaComponent lambdaComponent;

  @Override
  public void accept(LambdaFunctionStatusUpdateRequested event) {
    switch (event.getStatus()) {
      case SUSPEND -> lambdaComponent.disableFunction(event.getFunctionName());
      case ACTIVATE ->
          lambdaComponent.enableFunction(
              event.getFunctionName(), event.getFunctionReservedConcurrency());
    }
  }
}
