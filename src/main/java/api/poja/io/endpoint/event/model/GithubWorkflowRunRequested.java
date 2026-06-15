package api.poja.io.endpoint.event.model;

import java.time.Duration;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
@AllArgsConstructor
public class GithubWorkflowRunRequested extends PojaEvent {
  private String githubLogin;
  private String repositoryName;
  private String branchName;
  private RunnableGithubWorkflow toRun;
  private Map<String, String> workflowParameters;
  @Deprecated private Map<String, String> deprecatedWorkflowParameters;
  private String appInstallationId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }

  @Getter
  public enum RunnableGithubWorkflow {
    CD_COMPUTE("cd-compute.yml");
    private final String workflowId;

    RunnableGithubWorkflow(String workflowId) {
      this.workflowId = workflowId;
    }
  }
}
