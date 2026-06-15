package api.poja.io.service;

import api.poja.io.endpoint.rest.mapper.AppInstallationMapper;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.AppInstallationRepository;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.service.github.GithubService;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppInstallationService {
  private final AppInstallationRepository repository;
  private final AppInstallationMapper mapper;
  private final GithubService githubService;

  public List<AppInstallation> saveAllForUser(List<AppInstallation> toSave) {
    var updatedToSave =
        toSave.stream()
            .map(
                installation ->
                    repository
                        .findByUserIdAndOwnerGithubLogin(
                            installation.getUserId(), installation.getOwnerGithubLogin())
                        .map(
                            i ->
                                i.toBuilder()
                                    .ghId(installation.getGhId())
                                    .avatarUrl(installation.getAvatarUrl())
                                    .repositorySelection(installation.getRepositorySelection())
                                    .build())
                        .orElse(installation))
            .toList();
    return repository.saveAll(updatedToSave);
  }

  public List<AppInstallation> saveAll(List<AppInstallation> toSave) {
    var updatedToSave =
        toSave.stream()
            .map(
                installation ->
                    repository
                        .findByOrgIdAndOwnerGithubLogin(
                            installation.getOrgId(), installation.getOwnerGithubLogin())
                        .map(
                            i ->
                                i.toBuilder()
                                    .ghId(installation.getGhId())
                                    .avatarUrl(installation.getAvatarUrl())
                                    .repositorySelection(installation.getRepositorySelection())
                                    .build())
                        .orElse(installation))
            .toList();
    return repository.saveAll(updatedToSave);
  }

  public AppInstallation save(AppInstallation toSave) {
    var updatedToSave =
        repository
            .findByOrgIdAndOwnerGithubLogin(toSave.getOrgId(), toSave.getOwnerGithubLogin())
            .map(
                i ->
                    i.toBuilder()
                        .ghId(toSave.getGhId())
                        .avatarUrl(toSave.getAvatarUrl())
                        .repositorySelection(toSave.getRepositorySelection())
                        .build())
            .orElse(toSave);
    return repository.save(updatedToSave);
  }

  // fixme: Sync with Github App Installation
  public List<AppInstallation> findAllByUserId(String userId) {
    return repository.findAllByUserId(userId).stream()
        .map(this::updateRepositorySelectionIfNull)
        .filter(e -> e.getRepositorySelection() != null)
        .toList();
  }

  // fixme: Sync with Github App Installation
  public List<AppInstallation> findAllByOrgId(String orgId) {
    return repository.findAllByOrgId(orgId).stream()
        .map(this::updateRepositorySelectionIfNull)
        .filter(e -> e.getRepositorySelection() != null)
        .toList();
  }

  public Optional<AppInstallation> findById(String id) {
    return repository.findById(id).map(this::updateRepositorySelectionIfNull);
  }

  public boolean existsById(String id) {
    return repository.existsById(id);
  }

  public AppInstallation getById(String id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("AppInstallation#Id = " + id + " not found."));
  }

  private AppInstallation updateRepositorySelectionIfNull(AppInstallation installation) {
    if (installation.getRepositorySelection() == null) {
      try {
        var ghAppInstallation = githubService.getInstallationByGhId(installation.getGhId());
        return save(
            installation.toBuilder()
                .repositorySelection(ghAppInstallation.repositorySelection())
                .build());
      } catch (ApiException e) {
        // do nothing
      }
    }
    return installation;
  }
}
