package api.poja.io.conf;

import static org.mockito.Mockito.when;

import api.poja.io.aws.AwsConf;
import api.poja.io.aws.cloudformation.CloudformationComponent;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.file.bucket.BucketComponent;
import api.poja.io.service.jwt.JwtGenerator;
import api.poja.io.service.stripe.StripeService;
import api.poja.io.sys.platform.PlatformConf;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.regions.Region;

@Import(AppPemLoaderConf.class)
public class MockedThirdParties extends FacadeIT {
  @LocalServerPort protected int port;

  @MockBean protected AwsConf awsConfMock;
  @MockBean protected GithubComponent githubComponentMock;
  @MockBean protected CloudformationComponent cloudformationComponentMock;
  @MockBean protected BucketComponent bucketComponentMock;
  @MockBean protected JwtGenerator generatorMock;
  @MockBean protected EventProducer<?> eventProducerMock;
  @MockBean protected StripeService stripeServiceMock;
  @SpyBean protected PlatformConf platformConfSpy;

  @BeforeEach
  void setupAwsConf() {
    when(awsConfMock.getRegion()).thenReturn(Region.of("dummy-region"));
    when(awsConfMock.getAccountId()).thenReturn("01");
    when(awsConfMock.getCredentials()).thenReturn(null);
  }
}
