package api.poja.io.file;

import static api.poja.io.file.ApplicationImportFileType.ANALYSIS_RESULT;
import static api.poja.io.file.ApplicationImportFileType.ENV_VARS_FILE;
import static api.poja.io.file.ApplicationImportFileType.GRADLE_BUILD_FILE;
import static api.poja.io.file.ApplicationImportFileType.POJA_CONF_FILE;
import static api.poja.io.file.ApplicationImportFileType.ZIPPED_CODE;
import static api.poja.io.file.hash.FileHashAlgorithm.SHA256;
import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import api.poja.io.file.bucket.BucketComponent;
import api.poja.io.file.bucket.BucketConf;
import api.poja.io.file.hash.FileHash;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.gradle.GradleDist;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

@Component
@Slf4j
public class ExtendedBucketComponent {
  private static final String APPLICATION_ZIP_CONTENT_TYPE = "application/zip";
  private static final String TEMP_FILES_TAG = "temporary=true";
  public static final String TEMP_FILES_BUCKET_PREFIX = "tmp-";
  public static final String ENV_VARS_FILENAME = "env-vars.json";
  public static final String INITIAL_ZIPPED_CODE_FILENAME = "initial-zipped-code.zip";
  public static final String CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT =
      "converted-to-gradle-zipped-code-snapshot.zip";
  public static final String INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT =
      "integrated-generated-code-zipped-code-snapshot.zip";
  public static final String FORMATTED_ZIPPED_CODE_SNAPSHOT = "formatted-zipped-code-snapshot.zip";
  // _MUST_ contain build.gradle and settings.gradle
  public static final String INITIAL_ZIPPED_GRADLE_BUILD_TOOL_FILENAME =
      "initial-zipped-gradle-bt.zip";
  public static final String POST_CONVERSION_ZIPPED_GRADLE_BUILD_TOOL_FILENAME =
      "post-conversion-zipped-gradle-bt.zip";
  public static final String POST_CONFLICT_RESOLUTION_GRADLE_BUILD_TOOL_FILENAME =
      "post-conflict-resolution-zipped-gradle-bt.zip";
  public static final String APP_IMPORT_POJA_CONF_FILENAME = "poja-conf.yml";
  public static final String APP_LANG_ANALYSIS_RESULT_FILENAME =
      "app-language-analysis-result.json";
  public static final String BUILD_TOOL_ANALYSIS_RESULT_FILENAME =
      "build-tool-analysis-result.json";

  private final BucketComponent bucketComponent;
  private final BucketConf bucketConf;
  private final S3Client targetAccountS3Client;

  public ExtendedBucketComponent(
      BucketComponent bucketComponent,
      BucketConf bucketConf,
      @Qualifier("targetAccountS3Client") S3Client s3Client) {
    this.bucketComponent = bucketComponent;
    this.bucketConf = bucketConf;
    this.targetAccountS3Client = s3Client;
  }

  public final FileHash upload(File file, String key) {
    return bucketComponent.upload(file, key);
  }

  public final URI getPresignedPutObjectUri(String key, Duration expiration) {
    try {
      return bucketConf
          .getS3Presigner()
          .presignPutObject(
              PutObjectPresignRequest.builder()
                  .putObjectRequest(
                      req ->
                          req.bucket(bucketConf.getBucketName())
                              .key(key)
                              .contentType(APPLICATION_ZIP_CONTENT_TYPE)
                              .tagging(TEMP_FILES_TAG))
                  .signatureDuration(expiration)
                  .build())
          .url()
          .toURI();
    } catch (URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public boolean doesExist(String bucketKey) {
    try {
      HeadObjectRequest headObjectRequest =
          HeadObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build();

      HeadObjectResponse headObjectResponse =
          bucketConf.getS3Client().headObject(headObjectRequest);
      return headObjectResponse != null;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  public static String getUserBucketKey(
      String userId, String appId, String envId, FileType fileType, String filename) {
    return switch (fileType) {
      case POJA_CONF ->
          String.format("users/%s/apps/%s/envs/%s/poja-files/%s", userId, appId, envId, filename);
      case BUILT_PACKAGE ->
          String.format(
              "users/%s/apps/%s/envs/%s/built-packages/%s", userId, appId, envId, filename);
      case DEPLOYMENT_FILE ->
          String.format(
              "users/%s/apps/%s/envs/%s/deployment-files/%s", userId, appId, envId, filename);
    };
  }

  public static String getTemplateConfigBucketKey(String templateId) {
    return String.format("templates/%s/config.yml", templateId);
  }

  public static String getUserBucketKey(
      String userId, String appId, String envId, FileType fileType) {
    return switch (fileType) {
      case POJA_CONF -> String.format("users/%s/apps/%s/envs/%s/poja-files/", userId, appId, envId);
      case BUILT_PACKAGE ->
          String.format("users/%s/apps/%s/envs/%s/built-packages/", userId, appId, envId);
      case DEPLOYMENT_FILE ->
          String.format("users/%s/apps/%s/envs/%s/deployment-files/", userId, appId, envId);
    };
  }

  public static String getOrgBucketKey(
      String orgId, String appId, String envId, FileType fileType, String filename) {
    return switch (fileType) {
      case POJA_CONF ->
          String.format("orgs/%s/apps/%s/envs/%s/poja-files/%s", orgId, appId, envId, filename);
      case BUILT_PACKAGE ->
          String.format("orgs/%s/apps/%s/envs/%s/built-packages/%s", orgId, appId, envId, filename);
      case DEPLOYMENT_FILE ->
          String.format(
              "orgs/%s/apps/%s/envs/%s/deployment-files/%s", orgId, appId, envId, filename);
    };
  }

  public static String getOrgBucketKey(
      String orgId, String appId, String envId, FileType fileType) {
    return switch (fileType) {
      case POJA_CONF -> String.format("orgs/%s/apps/%s/envs/%s/poja-files/", orgId, appId, envId);
      case BUILT_PACKAGE ->
          String.format("orgs/%s/apps/%s/envs/%s/built-packages/", orgId, appId, envId);
      case DEPLOYMENT_FILE ->
          String.format("orgs/%s/apps/%s/envs/%s/deployment-files/", orgId, appId, envId);
    };
  }

  public static String getOrgBucketKey(String orgId, String appId, String envId) {
    return String.format("orgs/%s/apps/%s/envs/%s/poja-files/", orgId, appId, envId);
  }

  public static String getAppImportBucketKey(
      String orgId, String importId, ApplicationImportFileType fileType, String filename) {
    return switch (fileType) {
      case ENV_VARS_FILE ->
          String.format(
              "orgs/%s/imports/%s/%s/%s",
              orgId, importId, ENV_VARS_FILE.getDirectoryName(), filename);
      case GRADLE_BUILD_FILE ->
          String.format(
              "orgs/%s/imports/%s/%s/%s",
              orgId, importId, GRADLE_BUILD_FILE.getDirectoryName(), filename);
      case POJA_CONF_FILE ->
          String.format(
              "orgs/%s/imports/%s/%s/%s",
              orgId, importId, POJA_CONF_FILE.getDirectoryName(), filename);
      case ZIPPED_CODE ->
          String.format(
              "orgs/%s/imports/%s/%s/%s",
              orgId, importId, ZIPPED_CODE.getDirectoryName(), filename);
      case ANALYSIS_RESULT ->
          String.format(
              "orgs/%s/imports/%s/%s/%s",
              orgId, importId, ANALYSIS_RESULT.getDirectoryName(), filename);
    };
  }

  public static String getGradleDistBucketKey(GradleDist dist) {
    return String.format("gradle-dist/%s.zip", dist.filename());
  }

  public static String getPojaVersionChangelogBucketKey(PojaVersion version) {
    return String.format("poja-versions/%s.md", version.toHumanReadableValue());
  }

  public static String getTempBucketKey(String fileExtensionWithDot) {
    return String.format(TEMP_FILES_BUCKET_PREFIX + "%s", randomUUID() + fileExtensionWithDot);
  }

  public final File download(String key) {
    return bucketComponent.download(key);
  }

  public final FileHash getFileHash(String bucketKey) {
    HeadObjectRequest headObjectRequest =
        HeadObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build();

    String sha256 = bucketConf.getS3Client().headObject(headObjectRequest).checksumSHA256();
    return new FileHash(SHA256, sha256);
  }

  public URI presignGetObject(String key, Duration expiration) {
    try {
      return bucketComponent.presign(key, expiration).toURI();
    } catch (URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public FileHash moveFile(String from, String to) {
    GetObjectTaggingResponse taggingResponse =
        bucketConf
            .getS3Client()
            .getObjectTagging(
                GetObjectTaggingRequest.builder()
                    .bucket(bucketConf.getBucketName())
                    .key(from)
                    .build());
    List<Tag> tags = taggingResponse.tagSet();

    String tagString =
        tags.stream().map(tag -> tag.key() + "=" + tag.value()).collect(Collectors.joining("&"));
    var copy =
        bucketConf
            .getS3TransferManager()
            .copy(
                req ->
                    req.copyObjectRequest(
                            copyReq ->
                                copyReq
                                    .sourceKey(from)
                                    .sourceBucket(bucketConf.getBucketName())
                                    .destinationBucket(bucketConf.getBucketName())
                                    .destinationKey(to)
                                    .metadataDirective(MetadataDirective.COPY)
                                    .taggingDirective(TaggingDirective.REPLACE)
                                    .tagging(tagString))
                        .addTransferListener(LoggingTransferListener.create()));
    var copied = copy.completionFuture().join();
    return new FileHash(SHA256, copied.response().copyObjectResult().checksumSHA256());
  }

  public String deleteFile(String key) {
    bucketConf
        .getS3Client()
        .deleteObject(delReq -> delReq.bucket(bucketConf.getBucketName()).key(key));
    log.info("deleted {}", key);
    return key;
  }

  public void changeBucketPolicy(String bucketName, IamPolicy iamPolicy) {
    targetAccountS3Client.putBucketPolicy(req -> req.bucket(bucketName).policy(iamPolicy.toJson()));
  }
}
