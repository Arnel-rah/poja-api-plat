package api.poja.io.aws;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

@Getter
@Configuration
public class AwsConf {
  private final String accountId;
  private final Region region;
  private final String executionRoleArn;
  private final AwsCredentialsProvider credentials;

  public AwsConf(
      @Value("${aws.account.id}") String accountId,
      @Value("${aws.region}") Region region,
      @Value("${target.account.execution.role.arn}") String executionRoleArn) {
    this.accountId = accountId;
    this.region = region;
    this.executionRoleArn = executionRoleArn;
    this.credentials = buildCredentialsProvider();
  }

  private AwsCredentialsProvider buildCredentialsProvider() {
    StsClient stsClient = StsClient.builder().region(region).build();

    return StsAssumeRoleCredentialsProvider.builder()
        .stsClient(stsClient)
        .refreshRequest(
            AssumeRoleRequest.builder()
                .roleArn(executionRoleArn)
                .roleSessionName("targetAccountSession")
                .durationSeconds(3600)
                .build())
        .build();
  }

  public static String iamUsernameArn(String accountId, String username) {
    return "arn:aws:iam::%s:user/%s".formatted(accountId, username);
  }
}
