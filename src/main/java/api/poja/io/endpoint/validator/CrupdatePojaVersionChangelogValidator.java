package api.poja.io.endpoint.validator;

import static com.google.common.base.Strings.isNullOrEmpty;

import api.poja.io.endpoint.rest.model.CrupdatePojaVersionChangelogRequestBody;
import api.poja.io.model.exception.BadRequestException;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

@Component
public class CrupdatePojaVersionChangelogValidator
    implements BiConsumer<String, CrupdatePojaVersionChangelogRequestBody> {
  @Override
  public void accept(String pojaVersion, CrupdatePojaVersionChangelogRequestBody body) {
    var sj = new StringJoiner(". ");
    if (isNullOrEmpty(pojaVersion)) {
      sj.add("pojaVersion must not be null or empty");
    }
    if (body == null || body.getChangelogMd() == null) {
      sj.add("changelog must not be null");
    }

    if (sj.length() > 0) {
      throw new BadRequestException(sj.toString());
    }
  }
}
