package api.poja.io.service;

import static api.poja.io.file.ExtendedBucketComponent.getPojaVersionChangelogBucketKey;

import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileWriter;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.pojaConf.version.PojaVersionDto;
import api.poja.io.repository.PojaVersionRepository;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PojaVersionService {

  static final Duration CHANGELOG_FILE_URI_EXPIRATION = Duration.ofDays(1);

  private final ExtendedBucketComponent bucketComponent;
  private final PojaVersionRepository repository;
  private final FileWriter fileWriter;

  public List<PojaVersionDto> findAll() {
    return repository.findAll().stream().map(this::withChangelogUrl).toList();
  }

  public PojaVersion findByVersion(String pojaVersion) {
    var pojaVersionOpt = repository.findByVersion(pojaVersion);
    return pojaVersionOpt.orElseThrow(
        () -> new NotFoundException("Poja version " + pojaVersion + " not found."));
  }

  public PojaVersionDto updateChangelog(String pojaVersionString, String changelog) {
    var pojaVersion = findByVersion(pojaVersionString);
    var bucketKey = getPojaVersionChangelogBucketKey(pojaVersion);
    var file = fileWriter.apply(changelog.getBytes(), null);
    bucketComponent.upload(file, bucketKey);
    return withChangelogUrl(pojaVersion);
  }

  private PojaVersionDto withChangelogUrl(PojaVersion version) {
    return new PojaVersionDto(
        version.getMajor(),
        version.getMinor(),
        version.getPatch(),
        getPojaVersionChangelogUrl(version));
  }

  private @Nullable URI getPojaVersionChangelogUrl(PojaVersion version) {
    var bucketKey = getPojaVersionChangelogBucketKey(version);
    if (!bucketComponent.doesExist(bucketKey)) {
      return null;
    }
    return bucketComponent.presignGetObject(bucketKey, CHANGELOG_FILE_URI_EXPIRATION);
  }
}
