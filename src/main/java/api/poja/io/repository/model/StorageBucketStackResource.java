package api.poja.io.repository.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"storage_bucket_stack_resource\"")
@EqualsAndHashCode
@ToString
public class StorageBucketStackResource {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String bucketName;
  private String stackId;
  private Instant creationTimestamp;
  private String envId;
  private String appEnvDeplId;

  public String getArn() {
    String FILE_STORAGE_ARN_TEMPLATE = "arn:aws:s3:::%s";
    return FILE_STORAGE_ARN_TEMPLATE.formatted(bucketName);
  }

  public String getArnWithAllObjects() {
    String FILE_STORAGE_ARN_TEMPLATE = "arn:aws:s3:::%s/*";
    return FILE_STORAGE_ARN_TEMPLATE.formatted(bucketName);
  }

  public URI getUri(String region) {
    return URI.create(
        "https://%s.console.aws.amazon.com/s3/buckets/%s".formatted(region, bucketName));
  }
}
