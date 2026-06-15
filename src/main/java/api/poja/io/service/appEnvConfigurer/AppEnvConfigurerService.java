package api.poja.io.service.appEnvConfigurer;

import static api.poja.io.file.ExtendedBucketComponent.getOrgBucketKey;
import static api.poja.io.file.ExtendedBucketComponent.getUserBucketKey;
import static api.poja.io.file.FileType.POJA_CONF;
import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import api.poja.io.endpoint.rest.client.JSON;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.endpoint.rest.model.PojaConf;
import api.poja.io.endpoint.rest.model.PojaConf1;
import api.poja.io.endpoint.rest.model.PojaConf2;
import api.poja.io.endpoint.rest.model.PojaConf3;
import api.poja.io.endpoint.rest.model.PojaConf4;
import api.poja.io.endpoint.rest.model.PojaConf5;
import api.poja.io.endpoint.rest.model.PojaConf6;
import api.poja.io.endpoint.rest.model.PojaConf7;
import api.poja.io.endpoint.rest.model.PojaConf8;
import api.poja.io.endpoint.rest.model.PojaConf9;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.InternalServerErrorException;
import api.poja.io.repository.model.Environment;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.EnvDeploymentConfService;
import api.poja.io.service.UserSubscriptionService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfMapper;
import api.poja.io.service.appEnvConfigurer.model.UploadedVersionnedConf;
import api.poja.io.service.organization.OrganizationService;
import java.util.HashSet;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AppEnvConfigurerService {
  private final PojaConfFileMapper mapper;
  private final OrganizationService organizationService;
  private final ExtendedBucketComponent bucketComponent;
  private final EnvDeploymentConfService confService;
  private final PojaConfMapper pojaConfMapper;
  private final ConcurrencyConfigService concurrencyConfigService;
  private final UserSubscriptionService userSubscriptionService;

  @Transactional
  public UploadedVersionnedConf configureEnvironment(
      String orgId, String appId, Environment environment, OneOfPojaConf pojaConf) {
    String environmentId = environment.getId();
    String orgOwnerId = organizationService.getById(orgId).getOwnerId();
    var isPremium = userSubscriptionService.findActiveSubscriptionByUserId(orgOwnerId).isPresent();

    var previous =
        environment.getAppliedConfId() == null
            ? null
            : readConfig(
                orgId,
                orgOwnerId,
                appId,
                environmentId,
                confService.getById(environment.getAppliedConfId()).getPojaConfFileKey());
    api.poja.io.model.pojaConf.PojaConf previousAsDomain =
        previous == null ? null : fromRest(previous, orgOwnerId, appId);
    api.poja.io.model.pojaConf.PojaConf toAsDomain = fromRest(pojaConf, orgOwnerId, appId);

    var validatedFile = mapper.applyMigration(previousAsDomain, toAsDomain, isPremium);
    String nonFormattedFilename = validatedFile.getName();
    String formattedFilename =
        getOrgBucketKey(orgId, appId, environmentId, POJA_CONF, nonFormattedFilename);
    bucketComponent.upload(validatedFile, formattedFilename);

    PojaConf baseClass = (PojaConf) pojaConf.getActualInstance();
    PojaVersion pojaVersion =
        PojaVersion.fromHumanReadableValue(baseClass.getVersion())
            .orElseThrow(() -> new ApiException(SERVER_EXCEPTION, "unable to get poja version"));
    return new UploadedVersionnedConf(pojaConf, pojaVersion, nonFormattedFilename);
  }

  private api.poja.io.model.pojaConf.PojaConf fromRest(
      OneOfPojaConf pojaConf, String userId, String appId) {
    var actualInstance = pojaConf.getActualInstance();
    if (JSON.isInstanceOf(PojaConf9.class, actualInstance, new HashSet<>())) {
      PojaConf9 rest = (PojaConf9) actualInstance;
      return pojaConfMapper.toDomainPojaConf9(
          rest,
          concurrencyConfigService.pojaConf9ConcurrencyFrom(
              rest.getConcurrency(), rest.getCompute(), userId),
          userId,
          appId);
    }
    if (JSON.isInstanceOf(PojaConf8.class, actualInstance, new HashSet<>())) {
      PojaConf8 rest = (PojaConf8) actualInstance;
      return pojaConfMapper.toDomainPojaConf8(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId,
          appId);
    }
    if (JSON.isInstanceOf(PojaConf7.class, actualInstance, new HashSet<>())) {
      PojaConf7 rest = (PojaConf7) actualInstance;
      return pojaConfMapper.toDomainPojaConf7(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    if (JSON.isInstanceOf(PojaConf6.class, actualInstance, new HashSet<>())) {
      PojaConf6 rest = (PojaConf6) actualInstance;
      return pojaConfMapper.toDomainPojaConf6(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    if (JSON.isInstanceOf(PojaConf5.class, actualInstance, new HashSet<>())) {
      PojaConf5 rest = (PojaConf5) actualInstance;
      return pojaConfMapper.toDomainPojaConf5(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    if (JSON.isInstanceOf(PojaConf4.class, actualInstance, new HashSet<>())) {
      PojaConf4 rest = (PojaConf4) actualInstance;
      return pojaConfMapper.toDomainPojaConf4(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    if (JSON.isInstanceOf(PojaConf3.class, actualInstance, new HashSet<>())) {
      PojaConf3 rest = (PojaConf3) actualInstance;
      return pojaConfMapper.toDomainPojaConf3(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    if (JSON.isInstanceOf(PojaConf2.class, actualInstance, new HashSet<>())) {
      PojaConf2 rest = (PojaConf2) actualInstance;
      return pojaConfMapper.toDomainPojaConf2(
          rest,
          concurrencyConfigService.pojaConf2ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    if (JSON.isInstanceOf(PojaConf1.class, actualInstance, new HashSet<>())) {
      PojaConf1 rest = (PojaConf1) actualInstance;
      return pojaConfMapper.toDomainPojaConf1(
          rest,
          concurrencyConfigService.pojaConf1ConcurrencyFrom(rest.getConcurrency(), userId),
          userId);
    }
    throw new IllegalArgumentException("unknown PojaConf");
  }

  public OneOfPojaConf readConfig(
      String orgId, String appId, String environmentId, String filename) {
    String bucketKey = getConfigKey(orgId, appId, environmentId, filename);
    var file = bucketComponent.download(bucketKey);
    return mapper.readAsRest(file);
  }

  private OneOfPojaConf readConfig(
      String orgId, String orgOwnerId, String appId, String environmentId, String filename) {
    String bucketKey = getConfigKey(orgId, orgOwnerId, appId, environmentId, filename);
    var file = bucketComponent.download(bucketKey);
    return mapper.readAsRest(file);
  }

  public api.poja.io.model.pojaConf.PojaConf readConfigAsDomain(
      String orgId, String appId, String environmentId, String filename) {
    String bucketKey = getConfigKey(orgId, appId, environmentId, filename);
    var file = bucketComponent.download(bucketKey);
    return mapper.readAsDomain(file);
  }

  private String getConfigKey(String orgId, String appId, String envId, String filename) {
    Organization org = organizationService.getById(orgId);
    String orgBucketKey = getOrgBucketKey(orgId, appId, envId, POJA_CONF, filename);
    String userBucketKey = getUserBucketKey(org.getOwnerId(), appId, envId, POJA_CONF, filename);

    if (bucketComponent.doesExist(orgBucketKey)) {
      return orgBucketKey;
    } else if (bucketComponent.doesExist(userBucketKey)) {
      return userBucketKey;
    } else {
      throw new InternalServerErrorException(
          "config not found in S3 for org.Id = "
              + orgId
              + " app.Id = "
              + appId
              + " environment.Id = "
              + envId);
    }
  }

  private String getConfigKey(
      String orgId, String orgOwnerId, String appId, String envId, String filename) {
    String orgBucketKey = getOrgBucketKey(orgId, appId, envId, POJA_CONF, filename);
    String userBucketKey = getUserBucketKey(orgOwnerId, appId, envId, POJA_CONF, filename);

    if (bucketComponent.doesExist(orgBucketKey)) {
      return orgBucketKey;
    } else if (bucketComponent.doesExist(userBucketKey)) {
      return userBucketKey;
    } else {
      throw new InternalServerErrorException(
          "config not found in S3 for org.Id = "
              + orgId
              + " app.Id = "
              + appId
              + " environment.Id = "
              + envId);
    }
  }
}
