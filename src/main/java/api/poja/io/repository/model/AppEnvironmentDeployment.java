package api.poja.io.repository.model;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELING;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_PUSH_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_INITIATED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;

import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.TransitionResult;
import jakarta.persistence.*;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"app_environment_deployment\"")
@EqualsAndHashCode(callSuper = false)
@ToString
public class AppEnvironmentDeployment
    extends AbstractStateMachine<DeploymentStateEnum, DeploymentState> {
  public static final String TAG_HTML_URI_FORMAT = "https://github.com/%s/%s/releases/tag/%s";
  public static final String WORKFLOW_RUN_HTML_URI_FORMAT =
      "https://github.com/%s/%s/actions/runs/%s/attempts/%s";

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String appId;

  @JoinColumn(name = "env_id")
  @ManyToOne
  private Environment env;

  private String envDeplConfId;
  private String deployedUrl;
  @CreationTimestamp private Instant creationDatetime;

  @OneToMany(cascade = ALL, fetch = EAGER, mappedBy = "appEnvDeploymentId")
  @Builder.Default
  private List<DeploymentState> states = new ArrayList<>();

  public String getGhCommitBranch() {
    return env.getFormattedEnvironmentType();
  }

  private String ghCommitMessage;
  private String ghCommitSha;
  private String ghCommitUrl;
  private String ghRepoName;
  private String ghRepoOwnerName;
  private String ghCommitterName;
  private String ghCommitterEmail;
  private String ghCommitterId;
  private String ghCommitterAvatarUrl;
  private String ghCommitterLogin;
  private String ghCommitterType;
  private String ghTagMessage;
  private String ghTagName;
  private String ghWorkflowRunId;
  private String ghWorkflowRunAttempt;

  public URI getGhWorkflowUri() {
    return ghWorkflowRunId == null
        ? null
        : URI.create(
            WORKFLOW_RUN_HTML_URI_FORMAT.formatted(
                ghRepoOwnerName,
                ghRepoName,
                ghWorkflowRunId,
                ghWorkflowRunAttempt == null ? 1 : ghWorkflowRunAttempt));
  }

  public URI getGhTagHtmlUri() {
    return ghTagName == null
        ? null
        : URI.create(TAG_HTML_URI_FORMAT.formatted(ghRepoOwnerName, ghRepoName, ghTagName));
  }

  @Override
  protected List<DeploymentState> internalStates() {
    return states;
  }

  @Override
  protected void addState(DeploymentState state) {
    states.add(state);
  }

  @Override
  public TransitionResult<DeploymentStateEnum, DeploymentState> canTransitionTo(
      DeploymentState from, DeploymentState to) {
    var currentStatus = from.getProgressionStatus();
    var targetStatus = to.getProgressionStatus();
    if (CANCELED.equals(targetStatus)) {
      if (CANCELED.equals(currentStatus)) {
        from.setTimestamp(to.getTimestamp());
        return ok(from);
      }
      return ok(to);
    }
    if (CANCELING.equals(targetStatus)) {
      if (CANCELED.equals(currentStatus) || COMPUTE_STACK_DEPLOYED.equals(currentStatus)) {
        return ko();
      }
      return ok(to);
    }
    return switch (currentStatus) {
      case CANCELING -> ok(to);
      case CANCELED -> ko();
      case CODE_GENERATION_IN_PROGRESS -> {
        if (CODE_PUSH_IN_PROGRESS.equals(targetStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case CODE_PUSH_IN_PROGRESS -> {
        switch (targetStatus) {
          case CODE_PUSH_SUCCESS, CODE_PUSH_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case CODE_PUSH_SUCCESS -> {
        switch (targetStatus) {
          case DEPLOYMENT_WORKFLOW_IN_PROGRESS, DEPLOYMENT_WORKFLOW_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case DEPLOYMENT_WORKFLOW_IN_PROGRESS -> {
        switch (targetStatus) {
          case DEPLOYMENT_WORKFLOW_IN_PROGRESS -> {
            from.setTimestamp(to.getTimestamp());
            yield ok(from);
          }
          case TEMPLATE_FILE_CHECK_IN_PROGRESS, DEPLOYMENT_WORKFLOW_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case TEMPLATE_FILE_CHECK_IN_PROGRESS -> {
        switch (targetStatus) {
          case TEMPLATE_FILE_CHECK_FAILED,
              INDEPENDENT_STACKS_DEPLOYMENT_INITIATED,
              INDEPENDENT_STACKS_DEPLOYMENT_QUEUED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case INDEPENDENT_STACKS_DEPLOYMENT_QUEUED -> {
        if (INDEPENDENT_STACKS_DEPLOYMENT_INITIATED.equals(targetStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case INDEPENDENT_STACKS_DEPLOYMENT_INITIATED -> {
        if (targetStatus == INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS) {
          yield ok(to);
        }
        yield ko();
      }
      case INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS -> {
        switch (targetStatus) {
          case INDEPENDENT_STACKS_DEPLOYMENT_FAILED, INDEPENDENT_STACKS_DEPLOYED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case INDEPENDENT_STACKS_DEPLOYED -> {
        if (targetStatus == COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS) {
          yield ok(to);
        }
        yield ko();
      }
      case COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS -> {
        switch (targetStatus) {
          case COMPUTE_STACK_DEPLOYMENT_FAILED, COMPUTE_STACK_DEPLOYED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      default -> ko();
    };
  }
}
