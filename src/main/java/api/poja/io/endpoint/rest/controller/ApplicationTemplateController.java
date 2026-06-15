package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.ApplicationMapper;
import api.poja.io.endpoint.rest.mapper.ApplicationTemplateMapper;
import api.poja.io.endpoint.rest.mapper.EnvironmentMapper;
import api.poja.io.endpoint.rest.model.ApplicationTemplate;
import api.poja.io.endpoint.rest.model.CloneApplicationTemplateRequestBody;
import api.poja.io.endpoint.rest.model.CreateAndDeployAppResponse;
import api.poja.io.endpoint.rest.model.GetApplicationTemplatesResponse;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.ApplicationTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class ApplicationTemplateController {
  private final ApplicationTemplateService applicationTemplateService;
  private final ApplicationTemplateMapper applicationTemplateMapper;
  private final ApplicationService applicationService;
  private final ApplicationMapper applicationMapper;
  private final EnvironmentMapper environmentMapper;

  @GetMapping("/application-templates")
  public GetApplicationTemplatesResponse getApplicationTemplates() {
    var data =
        applicationTemplateService.findAll().stream()
            .map(applicationTemplateMapper::toRest)
            .toList();
    return new GetApplicationTemplatesResponse().data(data);
  }

  @GetMapping("/application-templates/{templateId}")
  public ApplicationTemplate getApplicationTemplateById(@PathVariable String templateId) {
    return applicationTemplateMapper.toRest(applicationTemplateService.getById(templateId));
  }

  @GetMapping("/application-templates/{templateId}/config")
  public OneOfPojaConf getApplicationTemplateConfigById(@PathVariable String templateId) {
    return applicationTemplateService.getConfig(templateId);
  }

  @PostMapping("/orgs/{orgId}/application-templates/{templateId}/clone")
  public CreateAndDeployAppResponse cloneApplicationTemplate(
      @PathVariable String orgId,
      @PathVariable String templateId,
      @RequestBody CloneApplicationTemplateRequestBody requestBody) {
    var dto = applicationService.cloneTemplate(orgId, templateId, requestBody);
    return new CreateAndDeployAppResponse()
        .application(applicationMapper.toRest(dto.application()))
        .environment(environmentMapper.toRest(dto.environment()));
  }
}
