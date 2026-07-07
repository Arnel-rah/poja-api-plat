package api.poja.io.model;

import api.poja.io.file.bucket.BucketComponent;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AppPemLoader {
  private final BucketComponent bucketComponent;
  private final String appPemBucketKey;
  private static final String UTF_8 = "UTF-8";
  private String rs256privateKey;

  public AppPemLoader(
      BucketComponent bucketComponent, @Value("${app.pem.bucket.key}") String appPemBucketKey) {
    this.bucketComponent = bucketComponent;
    this.appPemBucketKey = appPemBucketKey;
  }

  @PostConstruct
  public void init() throws IOException {
    this.rs256privateKey =
        Files.readString(Path.of("N:/nel-project/PROJET1/poja-api-plat/dummy.pem"));
  }
}
