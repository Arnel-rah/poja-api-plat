package api.poja.io.aws.iam;

import static java.util.UUID.randomUUID;
import static software.amazon.awssdk.regions.Region.EU_WEST_3;

import api.poja.io.aws.AwsConf;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.iam.IamClient;

@Slf4j
@Disabled
class LocalIamComponentTest {
  // Replace with /poja-app-deployer/${Env}/target-account/execution/role-arn
  String executionRoleArn = "";
  ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create("jc");
  IamClient iamClient =
      IamClient.builder().region(EU_WEST_3).credentialsProvider(credentialsProvider).build();
  CloudFormationClient cloudFormationClient =
      CloudFormationClient.builder()
          .region(EU_WEST_3)
          .credentialsProvider(credentialsProvider)
          .build();
  IamComponent component =
      new IamComponent(iamClient, new AwsConf("id", EU_WEST_3, executionRoleArn));

  @Test
  void createIamWithPassChangePolicy() {
    String substring = randomUUID().toString().substring(0, 8);
    String username = "mahefa-" + substring;
    var profile = component.createIam(username);
    log.info("username {}, password {}", profile.username(), profile.password());
  }

  @Test
  void attachRolePolicy() {
    iamClient.attachUserPolicy(
        req ->
            req.userName("mahefa-4b42aa43")
                .policyArn("arn:aws:iam::aws:policy/IAMUserChangePassword"));
  }
}
