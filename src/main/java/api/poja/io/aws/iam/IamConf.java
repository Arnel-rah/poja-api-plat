package api.poja.io.aws.iam;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.iam.IamClient;

@Configuration
@AllArgsConstructor
public class IamConf {
  private final AwsConf awsConf;

  @Bean
  public IamClient iamClient() {
    return IamClient.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
