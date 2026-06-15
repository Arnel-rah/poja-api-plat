package api.poja.io.aws.cloudformation;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;

@Configuration
@AllArgsConstructor
public class CloudformationConf {
  private final AwsConf awsConf;

  @Bean
  public CloudFormationClient getCloudformationClient() {
    return CloudFormationClient.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
