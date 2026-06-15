package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.APP_LANG_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.AppLanguageAnalysisRequested;
import api.poja.io.endpoint.event.model.BuildToolAnalysisRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.service.ApplicationImportLogService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AppLanguageAnalysisRequestedServiceTest extends MockedThirdParties {
  @Autowired private AppLanguageAnalysisRequestedService subject;
  @Autowired private ApplicationImportStateService applicationImportStateService;
  @Autowired private ApplicationImportService applicationImportService;
  @Autowired private ApplicationImportLogService applicationImportLogService;
  @MockBean private ExtendedBucketComponent mockedBucketComponent;
  private static final String JAVA_PROJECT_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/java_project.zip";
  private static final String NOT_JAVA_PROJECT_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/react_project.zip";

  @Test
  void should_mark_as_in_progress_and_successful_when_analysis_succeeds() {
    when(mockedBucketComponent.download(any())).thenReturn(getFile(JAVA_PROJECT_RESOURCE_PATH));
    var importId = "import_2";
    var orgId = "org_2";
    var event = new AppLanguageAnalysisRequested(importId, orgId);

    subject.accept(event);

    var actualStates = applicationImportStateService.getStatesByImportId(importId);
    var actualOldestState = actualStates.getLast();
    var actualOldestStateLogs =
        applicationImportLogService.getLogsByStateId(actualOldestState.getId());
    var actualLatestState = actualStates.getFirst();
    var actualLatestStateLogs =
        applicationImportLogService.getLogsByStateId(actualLatestState.getId());
    var actualUpdatedImport = applicationImportService.getById(importId);

    var event2 = new BuildToolAnalysisRequested(importId, orgId);
    verify(eventProducerMock, times(1)).accept(argThat(l -> l.equals(List.of(event2))));
    verify(mockedBucketComponent, times(1))
        .upload(any(), endsWith(APP_LANG_ANALYSIS_RESULT_FILENAME));

    assertEquals(
        APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS, actualOldestState.getProgressionStatus());
    assertTrue(actualOldestStateLogs.isEmpty());
    assertEquals(
        APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL, actualLatestState.getProgressionStatus());
    assertTrue(actualLatestStateLogs.isEmpty());
    assertEquals(IN_PROGRESS, actualUpdatedImport.getStatus());
  }

  @Test
  void should_mark_as_failed_when_analysis_fails() {
    when(mockedBucketComponent.download(any())).thenReturn(getFile(NOT_JAVA_PROJECT_RESOURCE_PATH));
    var importId = "import_3";
    var orgId = "org_3";
    var event = new AppLanguageAnalysisRequested(importId, orgId);

    subject.accept(event);

    var actualStates = applicationImportStateService.getStatesByImportId(importId);
    var actualOldestState = actualStates.getLast();
    var actualOldestStateLogs =
        applicationImportLogService.getLogsByStateId(actualOldestState.getId());
    var actualLatestState = actualStates.getFirst();
    var actualErrorLog =
        applicationImportLogService.getLogsByStateId(actualLatestState.getId()).getFirst();
    var actualUpdatedImport = applicationImportService.getById(importId);

    assertEquals(
        APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS, actualOldestState.getProgressionStatus());
    assertTrue(actualOldestStateLogs.isEmpty());
    assertEquals(
        APPLICATION_LANGUAGE_VERIFICATION_FAILED, actualLatestState.getProgressionStatus());
    assertEquals(ERROR, actualErrorLog.getType());
    assertTrue(actualErrorLog.getMessage().contains("src/main/java not found"));
    assertEquals(FAILED, actualUpdatedImport.getStatus());
  }

  @Test
  void should_mark_as_failed_when_analysis_throws_exception() {
    var importId = "import_4";
    var orgId = "org_4";
    var event = new AppLanguageAnalysisRequested(importId, orgId);

    subject.accept(event);

    var actualStates = applicationImportStateService.getStatesByImportId(importId);
    var actualOldestState = actualStates.getLast();
    var actualOldestStateLogs =
        applicationImportLogService.getLogsByStateId(actualOldestState.getId());
    var actualLatestState = actualStates.getFirst();
    var actualErrorLog =
        applicationImportLogService.getLogsByStateId(actualLatestState.getId()).getFirst();
    var actualUpdatedImport = applicationImportService.getById(importId);

    assertEquals(
        APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS, actualOldestState.getProgressionStatus());
    assertTrue(actualOldestStateLogs.isEmpty());
    assertEquals(
        APPLICATION_LANGUAGE_VERIFICATION_FAILED, actualLatestState.getProgressionStatus());
    assertEquals(ERROR, actualErrorLog.getType());
    assertTrue(actualErrorLog.getMessage().contains("Unexpected error during analysis"));
    assertEquals(FAILED, actualUpdatedImport.getStatus());
  }
}
