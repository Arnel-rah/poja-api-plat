package api.poja.io.endpoint.rest.mapper;

import api.poja.io.model.EnvVar;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EnvVarMapper {
  private final ObjectMapper objectMapper;

  public EnvVar fromRest(api.poja.io.endpoint.rest.model.EnvVar rest) {
    return new EnvVar(rest.getName(), rest.getValue(), rest.getTestValue());
  }

  public api.poja.io.endpoint.rest.model.EnvVar toRest(EnvVar domain) {
    return new api.poja.io.endpoint.rest.model.EnvVar()
        .name(domain.name())
        .value(domain.value())
        .testValue(domain.testValue());
  }

  @SneakyThrows
  public Set<EnvVar> toDomain(File envVarFile) {
    return objectMapper.readValue(envVarFile, new TypeReference<Set<EnvVar>>() {});
  }
}
