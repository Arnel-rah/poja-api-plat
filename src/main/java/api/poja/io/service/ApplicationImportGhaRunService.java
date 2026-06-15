package api.poja.io.service;

import static api.poja.io.repository.model.AppEnvironmentDeployment.WORKFLOW_RUN_HTML_URI_FORMAT;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.rest.model.UpdateApplicationImportStatesRequestBody;
import api.poja.io.repository.jpa.ApplicationImportGhaRunRepository;
import api.poja.io.repository.model.ApplicationImportGhaRun;
import api.poja.io.repository.model.enums.AppImportGhaRunJobName;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApplicationImportGhaRunService {
  private final ApplicationImportGhaRunRepository repository;

  public List<ApplicationImportGhaRun> getAllByImportId(String appImportId) {
    return repository.findAllByAppImportId(appImportId);
  }

  public ApplicationImportGhaRun save(
      String appImportId, UpdateApplicationImportStatesRequestBody updateAppImportStateReqBody) {
    var jobName =
        AppImportGhaRunJobName.fromValue(
            requireNonNull(updateAppImportStateReqBody.getJobName()).name());
    var runUri =
        String.format(
            WORKFLOW_RUN_HTML_URI_FORMAT,
            updateAppImportStateReqBody.getRepoOwner(),
            updateAppImportStateReqBody.getRepoName(),
            updateAppImportStateReqBody.getRunId(),
            updateAppImportStateReqBody.getAttemptNb().toString());
    var ghaRun =
        ApplicationImportGhaRun.builder()
            .id(randomUUID().toString())
            .jobName(jobName)
            .runUri(runUri)
            .appImportId(appImportId)
            .build();

    return repository.save(ghaRun);
  }
}
