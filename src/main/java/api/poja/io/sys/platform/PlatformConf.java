package api.poja.io.sys.platform;

import static api.poja.io.sys.platform.PlatformConf.Mode.IDP;
import static api.poja.io.sys.platform.PlatformConf.Mode.SAAS;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public record PlatformConf(@Value("${platform.mode}") Mode mode) {
  public enum Mode {
    SAAS,
    IDP
  }

  public boolean isSaas() {
    return SAAS == mode;
  }

  public boolean isIdp() {
    return IDP == mode;
  }
}
