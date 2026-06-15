package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.model.importer.TestMocks.domainEnvVarsWithTestValues;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class EnvVarMapperTest {
  private final EnvVarMapper subject = new EnvVarMapper(new ObjectMapper());

  @Test
  void toRest_from_json_file_ok() {
    var actual = subject.toDomain(getFile("files/env-vars.json"));

    assertEquals(domainEnvVarsWithTestValues(), actual);
  }
}
