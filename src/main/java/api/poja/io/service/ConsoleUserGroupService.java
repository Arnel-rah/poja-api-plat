package api.poja.io.service;

import api.poja.io.aws.iam.IamComponent;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.ConsoleUserGroupRepository;
import api.poja.io.repository.model.ConsoleUserGroup;
import api.poja.io.service.validator.ConsoleUserGroupThresholdValidator;
import java.util.Optional;
import java.util.Stack;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iam.model.LimitExceededException;

@Service
@AllArgsConstructor
public class ConsoleUserGroupService {
  private final ConsoleUserGroupRepository repository;
  private final ConsoleUserGroupThresholdValidator thresholdValidator;
  private final IamComponent iamComponent;

  public ConsoleUserGroup getById(String id) {
    return findById(id).orElseThrow(() -> new NotFoundException(""));
  }

  public Optional<ConsoleUserGroup> findById(String id) {
    return repository.findById(id);
  }

  public Stack<ConsoleUserGroup> findAvailablesByOrgId(String orgId) {
    return repository.findOneByOrgIdAndCurrentIsTrueAndArchivedIsFalse(orgId);
  }

  public ConsoleUserGroup createNewByUser(
      String userId, String consoleUserUsername, ConsoleUserGroup userGroup)
      throws LimitExceededException {
    thresholdValidator.accept(userId);
    iamComponent.createGroupAndAttachUserToGroup(consoleUserUsername, userGroup.getName());
    return repository.save(userGroup);
  }

  public ConsoleUserGroup save(ConsoleUserGroup userGroup) {
    return repository.save(userGroup);
  }
}
