package api.poja.io.service.pojaConfHandler;

import api.poja.io.endpoint.event.model.PojaConfUploaded;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.exception.NotImplementedException;
import api.poja.io.model.pojaConf.PojaConf;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@Slf4j
public class PojaConfUploadedHandlerFacade implements PojaConfUploadedHandler {

  private final PojaConfUploadedHandler pojaConf1UploadedHandler;
  private final PojaConfUploadedHandler pojaConf2UploadedHandler;
  private final PojaConfUploadedHandler pojaConf3UploadedHandler;
  private final PojaConfUploadedHandler pojaConf4UploadedHandler;
  private final PojaConfUploadedHandler pojaConf5UploadedHandler;
  private final PojaConfUploadedHandler pojaConf6UploadedHandler;
  private final PojaConfUploadedHandler pojaConf7UploadedHandler;
  private final PojaConfUploadedHandler pojaConf8UploadedHandler;
  private final PojaConfUploadedHandler pojaConf9UploadedHandler;
  private String userHome;

  public PojaConfUploadedHandlerFacade(
      @Qualifier("pojaConf1UploadedHandler") PojaConfUploadedHandler pojaConf1UploadedHandler,
      @Qualifier("pojaConf2UploadedHandler") PojaConfUploadedHandler pojaConf2UploadedHandler,
      @Qualifier("pojaConf3UploadedHandler") PojaConfUploadedHandler pojaConf3UploadedHandler,
      @Qualifier("pojaConf4UploadedHandler") PojaConfUploadedHandler pojaConf4UploadedHandler,
      @Qualifier("pojaConf5UploadedHandler") PojaConfUploadedHandler pojaConf5UploadedHandler,
      @Qualifier("pojaConf6UploadedHandler") PojaConfUploadedHandler pojaConf6UploadedHandler,
      @Qualifier("pojaConf7UploadedHandler") PojaConfUploadedHandler pojaConf7UploadedHandler,
      @Qualifier("pojaConf8UploadedHandler") PojaConfUploadedHandler pojaConf8UploadedHandler,
      @Qualifier("pojaConf9UploadedHandler") PojaConfUploadedHandler pojaConf9UploadedHandler) {
    this.pojaConf1UploadedHandler = pojaConf1UploadedHandler;
    this.pojaConf2UploadedHandler = pojaConf2UploadedHandler;
    this.pojaConf3UploadedHandler = pojaConf3UploadedHandler;
    this.pojaConf4UploadedHandler = pojaConf4UploadedHandler;
    this.pojaConf5UploadedHandler = pojaConf5UploadedHandler;
    this.pojaConf6UploadedHandler = pojaConf6UploadedHandler;
    this.pojaConf7UploadedHandler = pojaConf7UploadedHandler;
    this.pojaConf8UploadedHandler = pojaConf8UploadedHandler;
    this.pojaConf9UploadedHandler = pojaConf9UploadedHandler;
  }

  @PostConstruct
  public void init() {
    userHome = System.getProperty("user.home");
    System.setProperty("user.home", "/tmp");
  }

  @PreDestroy
  public void destroy() {
    System.setProperty("user.home", userHome);
  }

  @Transactional
  @Override
  public void accept(PojaConfUploaded pojaConfUploaded) {
    getPojaConfUploadedHandler(pojaConfUploaded.getPojaVersion()).accept(pojaConfUploaded);
  }

  @Override
  public boolean supports(PojaVersion pojaVersion) {
    throw new UnsupportedOperationException("method unused");
  }

  @Override
  public void configureCdCompute(Path clonedDirPath, PojaConf pojaConf) {
    getPojaConfUploadedHandler(pojaConf.getVersion()).configureCdCompute(clonedDirPath, pojaConf);
  }

  private PojaConfUploadedHandler getPojaConfUploadedHandler(PojaVersion pojaVersion) {
    if (pojaConf1UploadedHandler.supports(pojaVersion)) {
      return pojaConf1UploadedHandler;
    }
    if (pojaConf2UploadedHandler.supports(pojaVersion)) {
      return pojaConf2UploadedHandler;
    }
    if (pojaConf3UploadedHandler.supports(pojaVersion)) {
      return pojaConf3UploadedHandler;
    }
    if (pojaConf4UploadedHandler.supports(pojaVersion)) {
      return pojaConf4UploadedHandler;
    }
    if (pojaConf5UploadedHandler.supports(pojaVersion)) {
      return pojaConf5UploadedHandler;
    }
    if (pojaConf6UploadedHandler.supports(pojaVersion)) {
      return pojaConf6UploadedHandler;
    }
    if (pojaConf7UploadedHandler.supports(pojaVersion)) {
      return pojaConf7UploadedHandler;
    }
    if (pojaConf8UploadedHandler.supports(pojaVersion)) {
      return pojaConf8UploadedHandler;
    }
    if (pojaConf9UploadedHandler.supports(pojaVersion)) {
      return pojaConf9UploadedHandler;
    }
    throw new NotImplementedException(
        String.format(
            "No PojaConfUploadedHandler implementation found for Poja version: %s", pojaVersion));
  }
}
