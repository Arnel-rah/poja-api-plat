package api.poja.io.endpoint.rest.controller;

import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.ACTIVE;
import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.SUSPENDED;
import static api.poja.io.model.RangedInstant.getRangedInstant;
import static java.util.Objects.requireNonNull;

import api.poja.io.endpoint.rest.mapper.BillingInfoMapper;
import api.poja.io.endpoint.rest.mapper.EnvironmentMapper;
import api.poja.io.endpoint.rest.model.CrupdateEnvironmentsRequestBody;
import api.poja.io.endpoint.rest.model.CrupdateEnvironmentsResponse;
import api.poja.io.endpoint.rest.model.DeployEnvRequestBody;
import api.poja.io.endpoint.rest.model.DeployEnvResponse;
import api.poja.io.endpoint.rest.model.EnvBillingInfo;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.endpoint.rest.model.Environment.StatusEnum;
import api.poja.io.endpoint.rest.model.EnvironmentsResponse;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.UpdateEnvStatusRequestBody;
import api.poja.io.model.RangedInstant;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.EnvironmentService;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ApplicationEnvironmentController {
  private final EnvironmentService service;
  private final EnvironmentMapper mapper;
  private final BillingInfoService billingInfoService;
  private final BillingInfoMapper billingInfoMapper;

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/environments")
  public EnvironmentsResponse getApplicationEnvironments(
      @PathVariable String orgId, @PathVariable String applicationId) {
    var data =
        service.findAllNotArchivedByApplicationId(applicationId).stream()
            .map(mapper::toRest)
            .toList();
    return new EnvironmentsResponse().data(data);
  }

  @PutMapping("/orgs/{orgId}/applications/{applicationId}/environments")
  public CrupdateEnvironmentsResponse crupdateApplicationEnvironments(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @RequestBody CrupdateEnvironmentsRequestBody requestBody) {
    var requestBodyData = requireNonNull(requestBody.getData());
    var mappedData = requestBodyData.stream().map(a -> mapper.toDomain(applicationId, a)).toList();
    var data =
        service.crupdateEnvironments(orgId, applicationId, mappedData, true).stream()
            .map(mapper::toRest)
            .toList();
    return new CrupdateEnvironmentsResponse().data(data);
  }

  @PutMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/configs/{confId}")
  public EnvConf saveApplicationEnvConfig(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @RequestBody EnvConf requestBody) {
    var envConf = service.configureEnvironment(orgId, applicationId, environmentId, requestBody);
    return new EnvConf().conf(envConf.oneOfPojaConf()).id(envConf.id());
  }

  @PostMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/configs/{confId}/deploy")
  public DeployEnvResponse saveAppEnvConfAndDeploy(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @RequestBody EnvConf envConf) {
    return new DeployEnvResponse()
        .deploymentId(
            service.saveAppEnvConfAndDeploy(orgId, applicationId, environmentId, envConf));
  }

  @GetMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/configs/{confId}")
  public EnvConf getApplicationEnvironmentConfigById(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @PathVariable String confId) {
    var envConf = service.getConfig(orgId, applicationId, environmentId, confId);
    return new EnvConf().conf(envConf.oneOfPojaConf()).id(envConf.id());
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}")
  public Environment getApplicationEnvironmentById(
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @PathVariable String orgId) {
    return mapper.toRest(
        service.getUserApplicationEnvironmentById(orgId, applicationId, environmentId));
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/statuses")
  public Environment updateEnvStatus(
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @PathVariable String orgId,
      @RequestBody UpdateEnvStatusRequestBody requestBody) {
    return mapper.toRest(
        service.updateEnvStatusAsync(
            orgId,
            applicationId,
            environmentId,
            getStatusEnum(requireNonNull(requestBody.getAction()))));
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/billing")
  public EnvBillingInfo getUserAppEnvironmentBillingInfo(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @RequestParam(required = false) Instant startTime,
      @RequestParam(required = false) Instant endTime,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) MonthType month) {
    RangedInstant datetimeRange = getRangedInstant(startTime, endTime, year, month);
    return billingInfoMapper.toEnvRest(
        billingInfoService.getOrgBillingInfoByEnvironment(
            orgId, applicationId, environmentId, datetimeRange),
        datetimeRange.start(),
        datetimeRange.end());
  }

  @PostMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/deployments")
  public DeployEnvResponse deployEnv(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @RequestBody DeployEnvRequestBody requestBody) {
    return new DeployEnvResponse()
        .deploymentId(
            service.deployEnvWithConf(
                orgId, applicationId, environmentId, requestBody.getConfId()));
  }

  private static StatusEnum getStatusEnum(UpdateEnvStatusRequestBody.ActionEnum action) {
    return switch (action) {
      case ACTIVATE -> ACTIVE;
      case SUSPEND -> SUSPENDED;
    };
  }
}
