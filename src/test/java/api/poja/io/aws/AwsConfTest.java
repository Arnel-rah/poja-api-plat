package api.poja.io.aws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AwsConfTest {

  @Test
  void iamUsernameArn() {
    assertEquals("arn:aws:iam::fr:user/fr", AwsConf.iamUsernameArn("fr", "fr"));
  }
}
