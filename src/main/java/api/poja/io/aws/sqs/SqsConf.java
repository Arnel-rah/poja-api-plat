package api.poja.io.aws.sqs;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@AllArgsConstructor
public class SqsConf {
  private final AwsConf awsConf;

  @Bean("targetAccountSqsClient")
  public SqsClient sqsClient() {
    return SqsClient.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
