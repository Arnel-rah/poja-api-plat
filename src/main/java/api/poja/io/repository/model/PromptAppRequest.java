package api.poja.io.repository.model;

import api.poja.io.repository.model.enums.PromptAppRequestStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "prompt_app_request")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptAppRequest {

  @Id private String id;

  @Column(name = "org_id", nullable = false)
  private String orgId;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "prompt", nullable = false, length = 1000)
  private String prompt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private PromptAppRequestStatus status;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "application_name")
  private String applicationName;

  @Column(name = "error_message")
  private String errorMessage;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;
}
