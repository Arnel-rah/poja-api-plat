package api.poja.io.aws.cloudformation;

import static api.poja.io.file.ExtendedBucketComponent.getOrgBucketKey;
import static api.poja.io.file.ExtendedBucketComponent.getUserBucketKey;
import static api.poja.io.file.FileType.DEPLOYMENT_FILE;

import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.service.organization.OrganizationService;
import java.net.URI;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class CloudformationTemplateConf {
  private final ExtendedBucketComponent extendedBucketComponent;
  private final OrganizationService organizationService;
  public static final Duration TEMPLATE_PRESIGNED_URL_DURATION = Duration.ofMinutes(10);

  public URI getCloudformationTemplateUrl(
      String orgId, String appId, String envId, String filename) {
    var org = organizationService.getById(orgId);

    String formattedOrgBucketKey = getOrgBucketKey(orgId, appId, envId, DEPLOYMENT_FILE) + filename;
    String formattedUserBucketKey =
        getUserBucketKey(org.getOwnerId(), appId, envId, DEPLOYMENT_FILE) + filename;

    if (!extendedBucketComponent.doesExist(formattedOrgBucketKey)) {
      return extendedBucketComponent.presignGetObject(
          formattedUserBucketKey, TEMPLATE_PRESIGNED_URL_DURATION);
    }

    return extendedBucketComponent.presignGetObject(
        formattedOrgBucketKey, TEMPLATE_PRESIGNED_URL_DURATION);
  }
}
