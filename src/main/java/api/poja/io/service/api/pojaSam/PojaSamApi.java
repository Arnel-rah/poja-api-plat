package api.poja.io.service.api.pojaSam;

import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpMethod.PUT;

import api.poja.io.file.FileWriter;
import api.poja.io.model.PojaVersion;
import api.poja.io.service.api.pojaSam.model.CodeUri;
import java.io.File;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class PojaSamApi {
  public static final String X_API_KEY_HEADER_NAME = "x-api-key";
  private final RestTemplate restTemplate;
  private final FileWriter fileWriter;
  private final String apiKey;
  private final URI baseUri;

  public PojaSamApi(
      RestTemplate restTemplate,
      FileWriter fileWriter,
      @Value("${poja.sam.api.key}") String apiKey,
      @Value("${poja.sam.api.uri}") String baseUri) {
    this.restTemplate = restTemplate;
    this.fileWriter = fileWriter;
    this.apiKey = apiKey;
    this.baseUri = URI.create(baseUri);
  }

  private CodeUri generateFromApi(PojaVersion pojaVersion, File pojaConfFile) {
    HttpHeaders headers = getPojaSamApiHttpHeaders();
    MultipartBodyBuilder bodies = new MultipartBodyBuilder();
    bodies.part("conf", generatePojaConf(pojaConfFile));
    MultiValueMap<String, HttpEntity<?>> multipartBody = bodies.build();
    HttpEntity<MultiValueMap<String, HttpEntity<?>>> request =
        new HttpEntity<>(multipartBody, headers);
    URI formattedSamUri = getFormattedSamUri();

    log.info("downloading code from {}", formattedSamUri);
    return restTemplate.exchange(formattedSamUri, PUT, request, CodeUri.class).getBody();
  }

  private HttpHeaders getPojaSamApiHttpHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(X_API_KEY_HEADER_NAME, apiKey);
    return headers;
  }

  private URI getFormattedSamUri() {
    return UriComponentsBuilder.fromUri(baseUri).path("/gen").build().toUri();
  }

  /**
   * @param pojaVersion
   * @param pojaConfFile
   * @return returns a zip but typed as File so we can re-use at will
   */
  public File genCode(PojaVersion pojaVersion, File pojaConfFile) {
    var codeUri = generateFromApi(pojaVersion, pojaConfFile);
    return downloadFile(randomUUID().toString(), codeUri.uri());
  }

  private Resource generatePojaConf(File file) {
    return new FileSystemResource(file);
  }

  private File downloadFile(String filename, URI uri) {
    log.info("GET API CALL downloading {} from {}", filename, uri);
    var response = restTemplate.getForObject(uri, byte[].class);
    return fileWriter.apply(response, null);
  }
}
