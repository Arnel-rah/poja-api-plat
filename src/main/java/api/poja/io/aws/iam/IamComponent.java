package api.poja.io.aws.iam;

import static com.amazonaws.services.lambda.runtime.events.IamPolicyResponseV1.DENY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;

import api.poja.io.aws.AwsConf;
import api.poja.io.aws.iam.model.ConsoleUserCredentials;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.policybuilder.iam.IamValue;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.GetGroupPolicyResponse;
import software.amazon.awssdk.services.iam.model.GetUserPolicyResponse;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.iam.model.LimitExceededException;
import software.amazon.awssdk.services.iam.model.NoSuchEntityException;

@Component
@AllArgsConstructor
@Slf4j
public class IamComponent {
  private static final String USER_SUSPENSION_SID = "suspendusersid";
  private static final String USER_SUSPENSION_POLICY_NAME = "userSuspension";
  private final IamClient client;
  private final AwsConf awsConf;

  public ConsoleUserCredentials createIam(String username) throws IamException {
    var req1 = client.createUser(req -> req.userName(username));
    var createdUser = req1.user();
    String password = randomUUID().toString();
    var loginProfileResponse =
        client.createLoginProfile(
            req ->
                req.userName(createdUser.userName())
                    .password(password)
                    .passwordResetRequired(false));
    return new ConsoleUserCredentials(
        loginProfileResponse.loginProfile().userName(), password, awsConf.getAccountId());
  }

  public ConsoleUserCredentials updateUserPassword(String username) throws IamException {
    var user = client.getUser(req -> req.userName(username)).user();
    var newPassword = randomUUID().toString();
    client.updateLoginProfile(
        req -> req.userName(username).password(newPassword).passwordResetRequired(false));
    return new ConsoleUserCredentials(user.userName(), newPassword, awsConf.getAccountId());
  }

  public void createGroupAndAttachUserToGroup(String username, String groupName)
      throws LimitExceededException {
    client.createGroup(req -> req.groupName(groupName));
    attachUserToGroup(username, groupName);
  }

  public void attachUserToGroup(String username, String groupName) {
    client.addUserToGroup(req -> req.userName(username).groupName(groupName));
  }

  public void detachUserFromGroup(String username, String groupName) {
    client.removeUserFromGroup(req -> req.userName(username).groupName(groupName));
  }

  public void suspendUser(String username) {
    addInlinePolicyIfNotExists(
        username,
        USER_SUSPENSION_POLICY_NAME,
        IamPolicy.builder()
            .statements(
                List.of(
                    IamStatement.builder()
                        .sid(USER_SUSPENSION_SID)
                        .effect(DENY)
                        .addAction("*")
                        .addResource("*")
                        .build()))
            .build());
  }

  public void enableUser(String username) {
    Optional<IamPolicy> userIamPolicy = getUserIamPolicy(username, USER_SUSPENSION_POLICY_NAME);
    if (userIamPolicy.isPresent()) {
      client.deleteUserPolicy(
          req -> req.userName(username).policyName(USER_SUSPENSION_POLICY_NAME));
    }
  }

  public void addInlinePolicyIfNotExists(String username, String policyName, IamPolicy newPolicy) {
    var optionalIamPolicy = getUserIamPolicy(username, policyName);
    if (optionalIamPolicy.isEmpty()) {
      putUserPolicy(username, policyName, newPolicy);
      return;
    }
    var existingIamPolicy = optionalIamPolicy.get();
    var existingIamPolicyStatementsSids =
        existingIamPolicy.statements().stream().map(IamStatement::sid).toList();
    IamPolicy.Builder builder = existingIamPolicy.toBuilder();
    newPolicy
        .statements()
        .forEach(
            (t) -> {
              if (!existingIamPolicyStatementsSids.contains(t.sid())) {
                builder.addStatement(t);
              }
            });
    putUserPolicy(username, policyName, builder.build());
  }

  public Optional<IamPolicy> getUserIamPolicy(String username, String policyName) {
    var currentPolicy = getUserPolicy(username, policyName);
    return currentPolicy.map(this::readValueAsPolicy);
  }

  private Optional<GetUserPolicyResponse> getUserPolicy(String username, String policyName) {
    try {
      return Optional.of(
          client.getUserPolicy(req -> req.userName(username).policyName(policyName)));
    } catch (NoSuchEntityException e) {
      return empty();
    }
  }

  public void putUserPolicy(String username, String policyName, IamPolicy policy) {
    client.putUserPolicy(
        req -> req.userName(username).policyName(policyName).policyDocument(policy.toJson()));
  }

  @SneakyThrows
  private IamPolicy readValueAsPolicy(GetUserPolicyResponse currentPolicy) {
    var policy = URLDecoder.decode(currentPolicy.policyDocument(), UTF_8);
    return IamPolicyReader.create().read(policy);
  }

  public void putGroupPolicy(String groupName, String policyDocumentName, IamPolicy policy)
      throws LimitExceededException {
    client.putGroupPolicy(
        req ->
            req.groupName(groupName)
                .policyDocument(policy.toJson())
                .policyName(policyDocumentName));
  }

  public void deleteGroup(String groupName) {
    log.info("deleting group {}", groupName);
    var group = client.getGroup(req -> req.groupName(groupName));
    group
        .users()
        .forEach(
            u ->
                client.removeUserFromGroup(req -> req.userName(u.userName()).groupName(groupName)));
    // lists only 100 policies, should be enough, we only have one
    var policies = client.listGroupPolicies(req -> req.groupName(groupName));
    policies
        .policyNames()
        .forEach(p -> client.deleteGroupPolicy(req -> req.groupName(groupName).policyName(p)));
    client.deleteGroup(req -> req.groupName(groupName));
  }

  public Optional<IamPolicy> getUserGroupPolicy(String groupName, String policyName) {
    try {
      return Optional.of(
              client.getGroupPolicy(req -> req.groupName(groupName).policyName(policyName)))
          .map(this::readValueAsPolicy);
    } catch (NoSuchEntityException e) {
      return empty();
    }
  }

  public void crupdateGroupPolicyStatements(
      String groupName, String policyName, IamPolicy newPolicy) throws LimitExceededException {
    var optionalPolicy = getUserGroupPolicy(groupName, policyName);
    if (optionalPolicy.isEmpty()) {
      putGroupPolicy(groupName, policyName, newPolicy);
      return;
    }
    var existingPolicy = optionalPolicy.get();
    var existingStatements = existingPolicy.statements();

    List<IamStatement> updatedStatements =
        newPolicy.statements().stream()
            .map(
                newStatement ->
                    existingStatements.stream()
                        .filter(
                            existingStatement ->
                                isStatementSimilar(existingStatement, newStatement))
                        .findFirst()
                        .map(statement -> updateStatement(statement, newStatement))
                        .orElse(newStatement))
            .toList();

    IamPolicy updatedPolicy = existingPolicy.toBuilder().statements(updatedStatements).build();
    putGroupPolicy(groupName, policyName, updatedPolicy);
  }

  /**
   * Determines if two IAM statements are similar based on their `sid` or actions, making it easier
   * to merge or update them.
   *
   * <p>This method is useful for:
   *
   * <ul>
   *   <li>Handling cases where statements have the same actions but missing `sid` values.
   *   <li>Avoiding redundant permissions by treating similar statements as identical.
   *   <li>Simplifying IAM policy updates by ensuring logically identical statements are handled as
   *       the same.
   * </ul>
   *
   * @param a a statement
   * @param b a statement to be compared with {@code a} for similarity
   * @return {@code true} if the statements are similar, {@code false} otherwise
   */
  private static boolean isStatementSimilar(IamStatement a, IamStatement b) {
    if (a.sid() == null && b.sid() == null) {
      return a.actions().stream().anyMatch(b.actions()::contains);
    }
    return Objects.equals(a.sid(), b.sid());
  }

  private static IamStatement updateStatement(
      IamStatement oldStatement, IamStatement newStatement) {
    return oldStatement.toBuilder()
        .sid(newStatement.sid())
        .actions(mergeStatementPropertyValues(oldStatement.actions(), newStatement.actions()))
        .resources(mergeStatementPropertyValues(oldStatement.resources(), newStatement.resources()))
        .build();
  }

  private static <T extends IamValue> List<T> mergeStatementPropertyValues(
      List<T> currentValues, List<T> newValues) {
    Set<T> currentValuesCopy = new HashSet<>(currentValues);
    currentValuesCopy.addAll(newValues);
    return new ArrayList<>(currentValuesCopy);
  }

  @SneakyThrows
  private IamPolicy readValueAsPolicy(GetGroupPolicyResponse currentPolicy) {
    var policy = URLDecoder.decode(currentPolicy.policyDocument(), UTF_8);
    return IamPolicyReader.create().read(policy);
  }
}
