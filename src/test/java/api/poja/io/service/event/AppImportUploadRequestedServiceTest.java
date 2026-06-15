package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.service.git.GitUtils.cloneRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.AppImportUploadRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileWriter;
import api.poja.io.file.FileZipper;
import api.poja.io.service.git.GitUtils;
import api.poja.io.service.github.GithubService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class AppImportUploadRequestedServiceTest extends MockedThirdParties {
  @Autowired AppImportUploadRequestedService subject;
  @MockBean ExtendedBucketComponent extendedBucketMock;
  @MockBean FileWriter fileWriterMock;
  @MockBean FileZipper fileZipperMock;
  @MockBean GithubService githubServiceMock;
  MockedStatic<GitUtils> gitMock;
  private static final File envVarFile = getFile("files/env-vars.json");
  private static final String ENV_VAR_FILE_BUCKET_KEY =
      "orgs/org_1_id/imports/import_1/env-vars/env-vars.json";
  private static final String ZIPPED_CODE_BUCKET_KEY =
      "orgs/org_1_id/imports/import_1/zipped-codes/initial-zipped-code.zip";

  @Test
  void accept_calls_ok() {
    try (var gitMock = mockStatic(GitUtils.class)) {
      when(fileZipperMock.apply(any(), any(Path.class))).thenReturn(null);
      when(githubServiceMock.getInstallationToken(anyLong(), any())).thenReturn("dummy");
      when(fileWriterMock.apply(any(), any())).thenReturn(envVarFile);
      gitMock.when(() -> cloneRepository(any(), any(), any())).then(invocation -> null);
      var event =
          AppImportUploadRequested.builder()
              .appImport(pendingAppImport())
              .envVars(List.of())
              .build();

      subject.accept(event);

      verify(extendedBucketMock).upload(eq(envVarFile), eq(ENV_VAR_FILE_BUCKET_KEY));
      verify(extendedBucketMock).upload(any(), eq(ZIPPED_CODE_BUCKET_KEY));
    }
  }
}
