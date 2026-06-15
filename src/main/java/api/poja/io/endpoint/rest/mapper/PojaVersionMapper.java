package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.PojaVersion;
import org.springframework.stereotype.Component;

@Component
public class PojaVersionMapper {
  public PojaVersion toRest(api.poja.io.model.pojaConf.version.PojaVersionDto domain) {
    return new PojaVersion()
        .major(domain.major())
        .minor(domain.minor())
        .patch(domain.patch())
        .humanReadableValue(domain.toHumanReadableValue())
        .changelogUrl(domain.changelogUri());
  }
}
