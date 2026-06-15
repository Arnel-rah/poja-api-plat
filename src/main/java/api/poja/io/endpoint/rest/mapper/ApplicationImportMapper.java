package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationImport;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component("applicationImportRestMapper")
@AllArgsConstructor
public class ApplicationImportMapper {
  private final ApplicationImportStatusMapper applicationImportStatusMapper;

  public ApplicationImport toRest(api.poja.io.repository.model.ApplicationImport domain) {
    var status = applicationImportStatusMapper.toRest(domain.getStatus());

    return new ApplicationImport()
        .id(domain.getId())
        .name(domain.getAppName())
        .pojaVersion(domain.getPojaVersion())
        .status(status)
        .userId(domain.getUserId())
        .orgId(domain.getOrgId())
        .createdAppId(domain.getCreatedAppId())
        .githubRepositoryName(domain.getGithubRepositoryName())
        .githubRepositoryHttpUrl(domain.getGithubRepositoryHttpUrl())
        .githubRepositoryDefaultBranch(domain.getGithubRepositoryDefaultBranch());
  }
}
