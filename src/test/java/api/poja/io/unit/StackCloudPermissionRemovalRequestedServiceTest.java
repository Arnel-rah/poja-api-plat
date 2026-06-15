package api.poja.io.unit;

import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.service.event.StackCloudPermissionRemovalRequestedService.haveStatementsBeenUpdated;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;

class StackCloudPermissionRemovalRequestedServiceTest {
  private static final String POLICY_1_JSON_PATH = "files/policy_1.json";
  private static final String POLICY_2_JSON_PATH = "files/policy_2.json";

  @Test
  void compare_updated_policies_ok() {
    var initialPolicyStatements = parsePolicyFrom(POLICY_1_JSON_PATH).statements();
    var updatedPolicyStatements = parsePolicyFrom(POLICY_2_JSON_PATH).statements();

    assertTrue(haveStatementsBeenUpdated(initialPolicyStatements, updatedPolicyStatements));
  }

  @Test
  void compare_similar_policies_ok() {
    var initialPolicyStatements = parsePolicyFrom(POLICY_1_JSON_PATH).statements();

    assertFalse(haveStatementsBeenUpdated(initialPolicyStatements, initialPolicyStatements));
  }

  @SneakyThrows
  private static IamPolicy parsePolicyFrom(String path) {
    var json = getResource(path).getContentAsString(UTF_8);
    return IamPolicy.fromJson(json);
  }
}
