package api.poja.io.aws.costexplorer;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;

@AllArgsConstructor
@Configuration
public class CostExplorerConf {
  private final AwsConf awsConf;

  @Bean
  public CostExplorerClient costExplorer() {
    return CostExplorerClient.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
