package api.poja.io.service.event;

import static api.poja.io.repository.model.ComputeStackResource.computeLambdaFunctionArn;
import static api.poja.io.repository.model.ComputeStackResource.computeLogGroupArn;
import static api.poja.io.service.stackCrupdateCompleted.ComputeStackCrupdateCompletedService.CLOUDWATCH_LOGS_SID;
import static api.poja.io.service.stackCrupdateCompleted.ComputeStackCrupdateCompletedService.CLOUDWATCH_METRICS_SID;
import static api.poja.io.service.stackCrupdateCompleted.ComputeStackCrupdateCompletedService.LAMBDA_DASHBOARD_SID;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableSet;

import api.poja.io.aws.AwsConf;
import api.poja.io.aws.iam.IamComponent;
import api.poja.io.endpoint.event.model.StackCloudPermissionRemovalRequested;
import api.poja.io.repository.model.ComputeStackResource;
import api.poja.io.repository.model.ConsoleUserGroup;
import api.poja.io.repository.model.Organization;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.ComputeStackResourceService;
import api.poja.io.service.ConsoleUserGroupService;
import api.poja.io.service.organization.OrganizationService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.policybuilder.iam.IamValue;
import software.amazon.awssdk.regions.Region;

@AllArgsConstructor
@Service
@Slf4j
public class StackCloudPermissionRemovalRequestedService
    implements Consumer<StackCloudPermissionRemovalRequested> {
  private final IamComponent iamComponent;
  private final ComputeStackResourceService computeStackResourceService;
  private final OrganizationService organizationService;
  private final ConsoleUserGroupService consoleUserGroupService;
  private final AwsConf awsConf;

  @Override
  @Transactional
  public void accept(StackCloudPermissionRemovalRequested stackCloudPermissionRemovalRequested) {
    rmComputePolicies(
        stackCloudPermissionRemovalRequested.getOrgId(),
        stackCloudPermissionRemovalRequested.getComputeStacks());
  }

  private void rmComputePolicies(String orgId, List<Stack> computeStacks) {
    if (computeStacks.isEmpty()) {
      return;
    }
    Map<ConsoleUserGroup, List<String>> groupedByConsoleUserGroup =
        groupStacksByConsoleUserGroup(computeStacks);
    Region region = awsConf.getRegion();
    String accountId = awsConf.getAccountId();
    var org = getOrg(orgId);

    groupedByConsoleUserGroup.forEach(
        (consoleUserGroup, functionNames) ->
            updateStatements(consoleUserGroup, functionNames, region, accountId, org));
  }

  private void updateStatements(
      ConsoleUserGroup consoleUserGroup,
      List<String> functionNames,
      Region region,
      String accountId,
      Organization org) {
    FunctionNameArns arns = getFunctionNameArns(functionNames, region, accountId);
    if (consoleUserGroup == null) {
      return;
    }
    String userGroupName = consoleUserGroup.getName();
    Optional<IamPolicy> optionalUserPolicy =
        iamComponent.getUserGroupPolicy(userGroupName, org.getConsoleUserGroupPolicyDocumentName());
    if (optionalUserPolicy.isPresent()) {
      IamPolicy iamPolicy = optionalUserPolicy.get();
      List<IamStatement> currentStatements = iamPolicy.statements();

      var updatedStatements =
          updateStatements(currentStatements, arns.lambdaArns(), arns.logGroupArns());
      if (updatedStatements.stream().allMatch((e -> CLOUDWATCH_METRICS_SID.equals(e.sid())))) {
        consoleUserGroupService.save(
            consoleUserGroup.toBuilder().available(false).archived(true).build());
        iamComponent.deleteGroup(userGroupName);
      } else if (haveStatementsBeenUpdated(currentStatements, updatedStatements)) {
        consoleUserGroupService.save(consoleUserGroup.toBuilder().available(true).build());
        iamComponent.putGroupPolicy(
            userGroupName,
            org.getConsoleUserGroupPolicyDocumentName(),
            iamPolicy.toBuilder().statements(updatedStatements).build());
      }
    }
  }

  private Map<ConsoleUserGroup, List<String>> groupStacksByConsoleUserGroup(
      List<Stack> computeStacks) {
    List<ComputeStackResource> allResources =
        computeStackResourceService.findAllByStackIds(
            computeStacks.stream().map(Stack::getId).toList());

    List<ComputeStackResource> validGroupResources = new ArrayList<>();
    List<ComputeStackResource> nullGroupResources = new ArrayList<>();

    for (ComputeStackResource resource : allResources) {
      if (resource.getConsoleUserGroup() == null) {
        nullGroupResources.add(resource);
      } else {
        validGroupResources.add(resource);
      }
    }

    Map<ConsoleUserGroup, List<String>> groupedByConsoleUser =
        validGroupResources.stream()
            .collect(
                groupingBy(
                    ComputeStackResource::getConsoleUserGroup,
                    flatMapping(
                        r -> {
                          List<String> names = new ArrayList<>();
                          if (r.getFrontalFunctionName() != null) {
                            names.add(r.getFrontalFunctionName());
                          }
                          for (var workerFunction : r.getWorkerFunctions()) {
                            names.add(workerFunction.getName());
                          }
                          return names.stream();
                        },
                        collectingAndThen(Collectors.toSet(), ArrayList::new))));

    if (!nullGroupResources.isEmpty()) {
      nullGroupResources.forEach(
          r ->
              log.warn(
                  "Null ConsoleUserGroup for resource (frontal: {}, workers: {})",
                  r.getFrontalFunctionName(),
                  r.getWorkerFunctions()));
    }

    return groupedByConsoleUser;
  }

  private static FunctionNameArns getFunctionNameArns(
      List<String> functionNames, Region region, String accountId) {
    var lambdaArns =
        functionNames.stream()
            .map(fn -> computeLambdaFunctionArn(region, accountId, fn))
            .collect(toUnmodifiableSet());
    var logGroupArns =
        functionNames.stream()
            .map(fn -> computeLogGroupArn(region, accountId, fn))
            .collect(toUnmodifiableSet());
    return new FunctionNameArns(lambdaArns, logGroupArns);
  }

  private record FunctionNameArns(Set<String> lambdaArns, Set<String> logGroupArns) {}

  private static List<IamStatement> updateStatements(
      List<IamStatement> currentStatements, Set<String> lambdaArns, Set<String> logGroupArns) {
    return currentStatements.stream()
        .map(
            stm -> {
              if (LAMBDA_DASHBOARD_SID.equals(stm.sid())) {
                List<String> filteredList =
                    stm.resources().stream()
                        .map(IamValue::value)
                        .filter(not(lambdaArns::contains))
                        .toList();
                return filteredList.isEmpty()
                    ? null
                    : stm.toBuilder().resourceIds(filteredList).build();
              }
              if (CLOUDWATCH_LOGS_SID.equals(stm.sid())) {
                List<String> filteredList =
                    stm.resources().stream()
                        .map(IamValue::value)
                        .filter(not(logGroupArns::contains))
                        .toList();
                return filteredList.isEmpty()
                    ? null
                    : stm.toBuilder().resourceIds(filteredList).build();
              }
              return stm;
            })
        .filter(Objects::nonNull)
        .toList();
  }

  public static boolean haveStatementsBeenUpdated(
      List<IamStatement> initialStatements, List<IamStatement> updatedStatements) {
    var initialResourcesCount =
        initialStatements.stream()
            .mapToInt(st -> st.resources() == null ? 0 : st.resources().size())
            .sum();
    var updatedResourcesCount =
        updatedStatements.stream()
            .mapToInt(st -> st.resources() == null ? 0 : st.resources().size())
            .sum();

    return updatedResourcesCount < initialResourcesCount;
  }

  private Organization getOrg(String orgId) {
    return organizationService.getById(orgId);
  }
}
