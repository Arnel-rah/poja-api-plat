package api.poja.io.model.tag;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PojaTags {

  public static final String USER_ID = "poja:userid";
  public static final String APP_ID = "poja:appid";
  public static final String APP_NAME = "poja:appname";
  public static final String ENV = "poja:env";

  // Legacy tag keys (_MUST_ be maintained)
  public static final String LEGACY_APP = "app";
  public static final String LEGACY_USER_POJA = "user:poja";
  public static final String LEGACY_ENV = "env";

  private PojaTags() {}

  public static Map<String, String> from(String userId, String appId, String appName, String env) {
    Map<String, String> tags = new HashMap<>();
    tags.put(USER_ID, userId);
    tags.put(APP_ID, appId);
    tags.put(APP_NAME, appName);
    tags.put(ENV, env);
    tags.put(LEGACY_APP, appName);
    tags.put(LEGACY_USER_POJA, appName);
    tags.put(LEGACY_ENV, env);
    log.info("Created tags: {}", tags);
    return tags;
  }
}
