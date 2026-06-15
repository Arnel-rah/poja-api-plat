package api.poja.io.service.event;

import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.NONE;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_SUCCESSFUL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.BuildToolAnalysisRequested;
import api.poja.io.endpoint.event.model.PreTransformationTestRunRequested;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolAnalysisResult;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolAnalyzer;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolData;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import java.nio.file.Path;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class BuildToolAnalysisRequestedServiceTest {
  private BuildToolAnalysisRequestedService subject;
  final BuildToolAnalyzer analyzerMock = mock();
  final ApplicationImportService applicationImportServiceMock = mock();
  final ApplicationImportStateService applicationImportStateServiceMock = mock();
  final EventProducer<PreTransformationTestRunRequested> eventProducerMock = mock();

  private static final String ORG_ID = "org_1_id";
  private static final String IMPORT_ID = "import_1";
  private static final String ORG_5_ID = "org_5_id";
  private static final String IMPORT_5_ID = "import_5";

  @BeforeEach
  void setup() {
    subject =
        new BuildToolAnalysisRequestedService(
            applicationImportServiceMock,
            applicationImportStateServiceMock,
            analyzerMock,
            eventProducerMock);
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(strings = {"MAVEN", "GRADLE"})
  void should_mark_as_in_progress_and_successful_when_build_tool_analysis_succeeds(String bt) {
    String orgId = ORG_5_ID;
    String importId = IMPORT_5_ID;
    var result = new BuildToolAnalysisResult(Path.of("dummy"), BuildTool.valueOf(bt));

    when(analyzerMock.analyze(any(UnknownApplication.class))).thenReturn(result);
    var event = new BuildToolAnalysisRequested(importId, orgId);

    subject.accept(event);

    verify(applicationImportStateServiceMock)
        .updateState(IMPORT_5_ID, BUILD_TOOL_VERIFICATION_IN_PROGRESS);
    verify(applicationImportStateServiceMock)
        .updateState(IMPORT_5_ID, BUILD_TOOL_VERIFICATION_SUCCESSFUL);
    verify(applicationImportServiceMock)
        .uploadBuildToolAnalysisResultData(eq(orgId), eq(importId), any(BuildToolData.class));
  }

  @Test
  @SneakyThrows
  void should_mark_as_failed_when_build_tool_analysis_fails() {
    var result =
        new BuildToolAnalysisResult(
            Path.of("dummy"),
            NONE,
            ApplicationImportLog.error(
                "No build tool detected. Missing Gradle or Maven configuration files."));

    when(analyzerMock.analyze(any())).thenReturn(result);
    var event = new BuildToolAnalysisRequested(IMPORT_ID, ORG_ID);

    subject.accept(event);

    verify(applicationImportStateServiceMock)
        .updateState(IMPORT_ID, BUILD_TOOL_VERIFICATION_IN_PROGRESS);
    verify(applicationImportStateServiceMock)
        .updateState(
            eq(IMPORT_ID),
            eq(BUILD_TOOL_VERIFICATION_FAILED),
            argThat(
                logs ->
                    logs.size() == 1
                        && logs.getFirst()
                            .getMessage()
                            .equals(
                                "No build tool detected. Missing Gradle or Maven configuration"
                                    + " files.")
                        && logs.getFirst().getType().equals(ERROR)));
  }
}
