package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.PojaConfUploaded;
import api.poja.io.service.pojaConfHandler.PojaConfUploadedHandler;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PojaConfUploadedService implements Consumer<PojaConfUploaded> {
  public static final String POJA_BOT_USERNAME = "poja[bot]";
  @Deprecated public static final String JCLOUDIFY_BOT_USERNAME = "jcloudify[bot]";
  private final PojaConfUploadedHandler pojaConfUploadedHandler;

  @Override
  public void accept(PojaConfUploaded pojaConfUploaded) {
    pojaConfUploadedHandler.accept(pojaConfUploaded);
  }
}
