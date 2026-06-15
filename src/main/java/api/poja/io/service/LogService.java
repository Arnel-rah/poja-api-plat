package api.poja.io.service;

import static java.util.Comparator.comparing;

import api.poja.io.aws.cloudwatch.CloudwatchComponent;
import api.poja.io.endpoint.rest.model.EnvFunctionLog;
import api.poja.io.model.exception.NotFoundException;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;

@Service
@AllArgsConstructor
public class LogService {
  private final CloudwatchComponent cloudwatchComponent;

  public EnvFunctionLog getStackLogGroupByFunctionName(String functionName) {
    var existingLogs = cloudwatchComponent.getLambdaFunctionLogGroupsByNamePattern(functionName);
    if (existingLogs.isEmpty()) {
      throw new NotFoundException("No logs found for function name: " + functionName);
    }
    var filteredLogGroups =
        existingLogs.stream()
            .filter(l -> l.logGroupName().endsWith(functionName))
            .sorted(comparing(LogGroup::creationTime).reversed())
            .toList();
    if (filteredLogGroups.isEmpty()) {
      throw new NotFoundException("No logs found for function name: " + functionName);
    }
    LogGroup latestLogGroup = filteredLogGroups.getFirst();
    String logGroupName = latestLogGroup.logGroupName();
    return new EnvFunctionLog()
        .name(logGroupName)
        .creationDatetime(Instant.ofEpochMilli(latestLogGroup.creationTime()))
        .link(cloudwatchComponent.getLogGroupAllEventsUri(logGroupName));
  }
}
