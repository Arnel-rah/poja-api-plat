package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.endpoint.rest.mapper.ComputeStackResourceMapper.distinctByKey;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf2.PojaConf2.ScheduledTask;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasksValidator {

  private static final int MAX_SCHEDULE_NAME_LEN = 48;

  public void accept2(List<PojaConf2.ScheduledTask> scheduledTasks) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    var exceptionMessages = errorMessages2(scheduledTasks);
    if (!exceptionMessages.isEmpty()) {
      exceptionMessageBuilder.append(String.join(" ", exceptionMessages));
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept8(List<PojaConf8.ScheduledTask> scheduledTasks) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    var exceptionMessages = ScheduledTasksValidator.errorMessages8(scheduledTasks);
    if (!exceptionMessages.isEmpty()) {
      exceptionMessageBuilder.append(String.join(" ", exceptionMessages));
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept9(List<PojaConf9.ScheduledTask> scheduledTasks) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    var exceptionMessages = ScheduledTasksValidator.errorMessages9(scheduledTasks);
    if (!exceptionMessages.isEmpty()) {
      exceptionMessageBuilder.append(String.join(" ", exceptionMessages));
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public static Set<String> errorMessages2(List<PojaConf2.ScheduledTask> scheduledTasks) {
    Set<String> messages = new HashSet<>();
    var uniqueScheduledTasks =
        scheduledTasks.stream().filter(distinctByKey(ScheduledTask::name)).toList();
    if (uniqueScheduledTasks.size() < scheduledTasks.size()) {
      messages.add("scheduled tasks contains duplicate tasks by name.");
    }
    scheduledTasks.forEach(
        task -> {
          if (task.name() == null || task.name().isEmpty()) {
            messages.add("task.name is mandatory");
          } else if (task.name().length() > MAX_SCHEDULE_NAME_LEN) {
            messages.add("task.name must not exceed " + MAX_SCHEDULE_NAME_LEN + " characters.");
          }
          if (task.className() == null || task.className().isEmpty()) {
            messages.add("task.className is mandatory.");
          } else if (task.className().contains(".")) {
            messages.add("task.className must not contains \".\".");
          }
          if (task.description() == null || task.description().isEmpty()) {
            messages.add("task.description is mandatory.");
          }
          if (task.eventStackSource() == null) {
            messages.add("task.event_stack_source is mandatory.");
          }
          if (task.scheduleExpression() == null || task.scheduleExpression().isEmpty()) {
            messages.add("task.schedule_expression is mandatory.");
          }
        });
    return messages;
  }

  public static Set<String> errorMessages8(List<PojaConf8.ScheduledTask> scheduledTasks) {
    Set<String> messages = new HashSet<>();
    var uniqueScheduledTasks =
        scheduledTasks.stream().filter(distinctByKey(PojaConf8.ScheduledTask::name)).toList();
    if (uniqueScheduledTasks.size() < scheduledTasks.size()) {
      messages.add("scheduled tasks contains duplicate tasks by name.");
    }
    scheduledTasks.forEach(
        task -> {
          if (task.name() == null || task.name().isEmpty()) {
            messages.add("task.name is mandatory");
          } else if (task.name().length() > MAX_SCHEDULE_NAME_LEN) {
            messages.add("task.name must not exceed " + MAX_SCHEDULE_NAME_LEN + " characters.");
          }
          if (task.className() == null || task.className().isEmpty()) {
            messages.add("task.className is mandatory.");
          } else if (task.className().contains(".")) {
            messages.add("task.className must not contains \".\".");
          }
          if (task.description() == null || task.description().isEmpty()) {
            messages.add("task.description is mandatory.");
          }
          if (task.eventStackSource() == null) {
            messages.add("task.event_stack_source is mandatory.");
          }
          if (task.scheduleExpression() == null || task.scheduleExpression().isEmpty()) {
            messages.add("task.schedule_expression is mandatory.");
          }
        });
    return messages;
  }

  public static Set<String> errorMessages9(List<PojaConf9.ScheduledTask> scheduledTasks) {
    Set<String> messages = new HashSet<>();
    var uniqueScheduledTasks =
        scheduledTasks.stream().filter(distinctByKey(PojaConf9.ScheduledTask::name)).toList();
    if (uniqueScheduledTasks.size() < scheduledTasks.size()) {
      messages.add("scheduled tasks contains duplicate tasks by name.");
    }
    scheduledTasks.forEach(
        task -> {
          if (task.name() == null || task.name().isEmpty()) {
            messages.add("task.name is mandatory");
          } else if (task.name().length() > MAX_SCHEDULE_NAME_LEN) {
            messages.add("task.name must not exceed " + MAX_SCHEDULE_NAME_LEN + " characters.");
          }
          if (task.className() == null || task.className().isEmpty()) {
            messages.add("task.className is mandatory.");
          } else if (task.className().contains(".")) {
            messages.add("task.className must not contains \".\".");
          }
          if (task.description() == null || task.description().isEmpty()) {
            messages.add("task.description is mandatory.");
          }
          if (task.eventStackSource() == null) {
            messages.add("task.event_stack_source is mandatory.");
          }
          if (task.scheduleExpression() == null || task.scheduleExpression().isEmpty()) {
            messages.add("task.schedule_expression is mandatory.");
          }
        });
    return messages;
  }
}
