package api.poja.io.aws.cloudwatch;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;

@Configuration
@AllArgsConstructor
public class CloudwatchConf {
  private final AwsConf awsConf;

  @Bean
  public CloudWatchLogsClient getCloudwatchLogsClient() {
    return CloudWatchLogsClient.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
