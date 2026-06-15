package api.poja.io.aws.lambda;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
@AllArgsConstructor
public class LambdaConf {
  private final AwsConf awsConf;

  @Bean
  public LambdaClient lambdaClient() {
    return LambdaClient.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
