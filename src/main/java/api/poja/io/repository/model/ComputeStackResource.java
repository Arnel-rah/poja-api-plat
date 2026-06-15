package api.poja.io.repository.model;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.regions.Region;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"compute_resources\"")
@EqualsAndHashCode
@ToString
public class ComputeStackResource {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String environmentId;
  private String appEnvDeplId;
  private String frontalFunctionName;
  private Integer frontalFunctionReservedConcurrency;

  @Column(updatable = false)
  private boolean frontalFunctionDeleted;

  @ElementCollection(fetch = EAGER)
  @CollectionTable(
      name = "compute_stack_resource_worker_function_names",
      joinColumns = @JoinColumn(name = "compute_stack_resource_id"))
  @OrderColumn(name = "worker_index")
  private List<WorkerFunction> workerFunctions;

  private Instant creationDatetime;
  private String stackId;

  @ManyToOne
  @JsonIgnoreProperties("computeStackResources")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private ConsoleUserGroup consoleUserGroup;

  private static final String AWS_LAMBDA_LOG_GROUP_ARN =
      "arn:aws:logs:%s:%s:log-group:/aws/lambda/%s:*";
  private static final String AWS_LAMBDA_FUNCTION_ARN = "arn:aws:lambda:%s:%s:function:%s";

  public static String computeLogGroupArn(Region region, String accountId, String functionName) {
    return AWS_LAMBDA_LOG_GROUP_ARN.formatted(region, accountId, functionName);
  }

  public static String computeLambdaFunctionArn(
      Region region, String accountId, String functionName) {
    return AWS_LAMBDA_FUNCTION_ARN.formatted(region, accountId, functionName);
  }
}
