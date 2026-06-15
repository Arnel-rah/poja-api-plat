package api.poja.io.repository.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
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
@Table(name = "\"console_user_group\"")
@EqualsAndHashCode
@ToString
/*
 * One ConsoleUserGroup refers to AWS's UserGroup.
 * Currently (24th July 2025), AWS only supports 10 log policies per user group
 * but, for inline log policies, there is a limit of 5320 characters,
 * shared amongst all inline policies of this same group.
 * NB: we don't use managed policies, as we need a very particular granular control
 * for an optimal use, we will only keep 1 inline log policy per user group.
 * NICE TO KNOW:
 * for an app of 20 chars name (preprod-t-compute- +20 characters long name-+FunctionType),
 * one console user group log policy is enough to fit 6 environments = 3 applications
 */
public class ConsoleUserGroup {
  public static final int MAX_SUPPORTED_APP_NB_PER_INLINE_LOG_POLICIES_PER_USER_GROUP = 3;
  public static final int MAX_SUPPORTED_INLINE_LOG_POLICIES_PER_USER_GROUP = 1;

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private boolean available;
  private String name;
  private String userId;
  private String orgId;
  private boolean archived;

  @OneToMany(mappedBy = "consoleUserGroup")
  @JsonIgnoreProperties("consoleUserGroup")
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private List<ComputeStackResource> computeStackResources;
}
