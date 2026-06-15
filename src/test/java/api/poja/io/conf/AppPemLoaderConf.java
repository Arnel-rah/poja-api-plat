package api.poja.io.conf;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.model.AppPemLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AppPemLoaderConf {
  private static final String PEM_KEY = "rs256privatekey";

  @Bean
  public AppPemLoader appPemLoader() {
    AppPemLoader mock = mock(AppPemLoader.class);
    when(mock.getRs256privateKey()).thenReturn(PEM_KEY);
    return mock;
  }
}
