package api.poja.io.service.stack;

import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.StackRepository;
import api.poja.io.repository.jpa.dao.StackDao;
import api.poja.io.repository.model.Stack;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class StackService {
  private final StackRepository repository;
  private final StackDao dao;

  @Transactional
  public Page<Stack> findAllBy(
      String orgId,
      String applicationId,
      String environmentId,
      String appEnvDeplId,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    var data =
        findAllBy(
                orgId,
                applicationId,
                environmentId,
                appEnvDeplId,
                PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue()))
            .stream()
            .toList();
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  public Optional<Stack> findLatestByCriteria(
      String applicationId, String environmentId, StackType type, boolean archived) {
    return dao.findLatestByCriteria(applicationId, environmentId, type, archived);
  }

  public List<Stack> findAllBy(
      String orgId,
      String applicationId,
      String environmentId,
      String appEnvDeplId,
      Pageable pageable) {
    return dao
        .findAllByCriteria(orgId, applicationId, environmentId, appEnvDeplId, pageable)
        .stream()
        .toList();
  }

  public List<Stack> findAllByEnvId(String envId) {
    return repository.findAllByEnvironmentId(envId);
  }

  public Stack getById(String orgId, String applicationId, String environmentId, String stackId) {
    assert orgId != null;
    return repository
        .findByApplicationIdAndEnvironmentIdAndId(applicationId, environmentId, stackId)
        .orElseThrow(() -> new NotFoundException("Stack id=" + stackId + " not found"));
  }

  public Stack save(Stack toSave) {
    return repository.save(toSave);
  }

  public Stack archiveStack(Stack stack) {
    stack.setArchived(true);
    return save(stack);
  }

  public void archiveStacks(List<Stack> toArchive) {
    repository.saveAll(toArchive.stream().map(s -> s.toBuilder().archived(true).build()).toList());
  }

  public List<Stack> getAllByApplicationId(String applicationId) {
    return repository.findAllByApplicationId(applicationId);
  }

  public boolean existsByNameAndArchived(String name, boolean archived) {
    return repository.existsByNameAndArchived(name, archived);
  }

  public boolean existsByNameAndUserIdAndArchived(String name, String userId, boolean archived) {
    return repository.existsByNameAndUserIdAndArchived(name, userId, archived);
  }
}
