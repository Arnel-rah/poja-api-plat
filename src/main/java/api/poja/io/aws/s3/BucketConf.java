package api.poja.io.aws.s3;

import api.poja.io.aws.AwsConf;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@AllArgsConstructor
@Configuration("targetAccountBucketConf")
public class BucketConf {
  private final AwsConf awsConf;

  @Bean("targetAccountS3Client")
  public S3Client getS3Client() {
    return S3Client.builder()
        .region(awsConf.getRegion())
        .credentialsProvider(awsConf.getCredentials())
        .build();
  }
}
