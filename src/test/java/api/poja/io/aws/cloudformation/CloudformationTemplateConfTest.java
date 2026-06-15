package api.poja.io.aws.cloudformation;

import static api.poja.io.aws.cloudformation.CloudformationTemplateConf.TEMPLATE_PRESIGNED_URL_DURATION;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.file.ExtendedBucketComponent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class CloudformationTemplateConfTest extends MockedThirdParties {
  @MockBean ExtendedBucketComponent extendedBucketComponentMock;
  @Autowired private CloudformationTemplateConf cloudformationTemplateConf;

  @Test
  void getCloudformationTemplateUrlFromUser() {
    when(extendedBucketComponentMock.doesExist(any())).thenReturn(false);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    cloudformationTemplateConf.getCloudformationTemplateUrl(
        JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, POJA_APPLICATION_ENVIRONMENT_ID, "filename");

    verify(extendedBucketComponentMock, times(1))
        .presignGetObject(captor.capture(), eq(TEMPLATE_PRESIGNED_URL_DURATION));
    assertTrue(captor.getValue().contains("users"));
    assertFalse(captor.getValue().contains("orgs"));
  }

  @Test
  void getCloudformationTemplateUrlFromOrg() {
    reset(extendedBucketComponentMock);
    when(extendedBucketComponentMock.doesExist(any())).thenReturn(true);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

    cloudformationTemplateConf.getCloudformationTemplateUrl(
        JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, POJA_APPLICATION_ENVIRONMENT_ID, "filename");

    verify(extendedBucketComponentMock, times(1))
        .presignGetObject(captor.capture(), eq(TEMPLATE_PRESIGNED_URL_DURATION));
    assertTrue(captor.getValue().contains("orgs"));
    assertFalse(captor.getValue().contains("users"));
  }
}
