package api.poja.io.model.pojaConf.conf3;

import static api.poja.io.model.PojaVersion.POJA_3;

import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.NetworkingConfig;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;

@JsonPropertyOrder(alphabetic = true)
@Builder
public record PojaConf3(
    @JsonProperty("general") PojaConf2.General general,
    @JsonProperty("integration") PojaConf1.Integration integration,
    @JsonProperty("gen_api_client") PojaConf1.GenApiClient genApiClient,
    @JsonProperty("concurrency") PojaConf2Concurrency concurrency,
    @JsonProperty("compute") PojaConf2.Compute compute,
    @JsonProperty("emailing") PojaConf1.MailingConf mailing,
    @JsonProperty("testing") PojaConf1.TestingConf testing,
    @JsonProperty("database") PojaConf2.Database database,
    @JsonProperty("networking") NetworkingConfig networking,
    @JsonProperty("scheduled_tasks") List<PojaConf2.ScheduledTask> scheduledTasks)
    implements PojaConf {

  @JsonGetter
  public String version() {
    return getVersion().toHumanReadableValue();
  }

  @Override
  public PojaVersion getVersion() {
    return POJA_3;
  }
}
