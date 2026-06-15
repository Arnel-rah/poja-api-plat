package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationTemplate;
import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class ApplicationTemplateMapper {

  public ApplicationTemplate toRest(api.poja.io.repository.model.ApplicationTemplate domain) {
    return new ApplicationTemplate()
        .id(domain.getId())
        .name(domain.getName())
        .description(domain.getDescription())
        .repositoryUrl(
            domain.getRepositoryUrl() != null ? URI.create(domain.getRepositoryUrl()) : null)
        .demoUrl(domain.getDemoUrl() != null ? URI.create(domain.getDemoUrl()) : null)
        .withCustomConfig(domain.isWithCustomConfig());
  }
}
