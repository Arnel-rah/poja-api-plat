package api.poja.io.service;

import static api.poja.io.file.ExtendedBucketComponent.getTemplateConfigBucketKey;

import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.ApplicationTemplateRepository;
import api.poja.io.repository.model.ApplicationTemplate;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApplicationTemplateService {
  private final PojaConfFileMapper confFileMapper;
  private final ApplicationTemplateRepository repository;
  private final ExtendedBucketComponent bucketComponent;

  private Optional<ApplicationTemplate> findById(String id) {
    return repository.findById(id);
  }

  public ApplicationTemplate getById(String id) {
    return findById(id)
        .orElseThrow(
            () -> new NotFoundException("ApplicationTemplate with id " + id + " not found"));
  }

  public List<ApplicationTemplate> findAll() {
    return repository.findAll();
  }

  public OneOfPojaConf getConfig(String templateId) {
    var template = getById(templateId);

    if (!template.isWithCustomConfig()) {
      throw new BadRequestException("Template.id=" + templateId + " has no custom config.");
    }

    String bucketKey = getTemplateConfigBucketKey(template.getId());
    var file = bucketComponent.download(bucketKey);
    return confFileMapper.readAsRest(file);
  }
}
