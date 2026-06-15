package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.mapper.ComputeStackResourceMapper.distinctByKey;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE;
import static java.util.stream.Collectors.groupingBy;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppArchivalRequested;
import api.poja.io.endpoint.event.model.StackCloudPermissionRemovalRequested;
import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Environment;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.ApplicationImportArchivalService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.stack.StackService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AppArchivalRequestedService implements Consumer<AppArchivalRequested> {
  private final EnvironmentService environmentService;
  private final StackService stackService;
  private final EventProducer<StackCloudPermissionRemovalRequested> eventProducer;
  private final ApplicationService applicationService;
  private final ApplicationImportArchivalService importArchivalService;

  @Override
  @Transactional
  public void accept(AppArchivalRequested appArchivalRequested) {
    var applicationId = appArchivalRequested.getAppId();
    var orgId = appArchivalRequested.getOrgId();

    archiveAppImportIfExists(applicationId);

    List<Environment> applicationEnvironments =
        environmentService.findAllNotArchivedByApplicationId(applicationId);
    environmentService.crupdateEnvironments(
        orgId,
        applicationId,
        applicationEnvironments.stream().map(Environment::archiveSelf).toList(),
        false);
    fireCloudPermissionRemovalEvents(applicationId);
  }

  private void archiveAppImportIfExists(String applicationId) {
    var app = applicationService.getById(applicationId);
    if (app.getImportId() != null) {
      try {
        importArchivalService.archiveById(app.getImportId());
        log.info(
            "Archived ApplicationImport.id={} because Application.id={} was archived",
            app.getImportId(),
            app.getId());
      } catch (BadRequestException e) {
        log.info("ApplicationImport.id={} already archived, skipping", app.getImportId());
      } catch (NotFoundException e) {
        log.info("ApplicationImport.id={} not found, skipping", app.getImportId());
      }
    }
  }

  private void fireCloudPermissionRemovalEvents(String appId) {
    Application app = applicationService.getById(appId);
    Map<StackType, List<Stack>> stacks =
        stackService.getAllByApplicationId(appId).stream()
            .filter(s -> COMPUTE.equals(s.getType()))
            .filter(distinctByKey(Stack::getCfStackId))
            .collect(groupingBy(Stack::getType));
    var computeStacks = stacks.getOrDefault(COMPUTE, List.of());
    if (!computeStacks.isEmpty()) {
      eventProducer.accept(
          List.of(
              StackCloudPermissionRemovalRequested.builder()
                  .orgId(app.getOrgId())
                  .computeStacks(computeStacks)
                  .build()));
    }
  }
}
