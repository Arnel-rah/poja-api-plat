package api.poja.io.endpoint.event.model;

import api.poja.io.model.EnvVar;
import api.poja.io.repository.model.ApplicationImport;
import java.time.Duration;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder(toBuilder = true)
public class AppImportUploadRequested extends PojaEvent {
  private final ApplicationImport appImport;
  private final List<EnvVar> envVars;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
