package api.poja.io.service.stack;

import static api.poja.io.service.event.StackCrupdatedService.mergeAndSortStackEventList;
import static java.io.File.createTempFile;

import api.poja.io.aws.cloudformation.CloudformationComponent;
import api.poja.io.endpoint.rest.mapper.StackMapper;
import api.poja.io.endpoint.rest.model.StackEvent;
import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.StackEventData;
import api.poja.io.model.exception.InternalServerErrorException;
import api.poja.io.model.page.Page;
import api.poja.io.model.page.Paginator;
import api.poja.io.service.organization.OrganizationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StackEventService {
  public static final String STACK_EVENT_FILENAME = "log.json";
  private final OrganizationService organizationService;
  private final CloudformationComponent cloudformationComponent;
  private final ExtendedBucketComponent bucketComponent;
  private final StackMapper mapper;
  private final ObjectMapper om;
  private final Paginator paginator;

  public static String getOrgStackEventsBucketKey(
      String orgId, String appId, String envId, StackType stackType) {
    return String.format(
        "orgs/%s/apps/%s/envs/%s/stacks/%s/events/%s",
        orgId, appId, envId, stackType.getValue().toUpperCase(), STACK_EVENT_FILENAME);
  }

  public static String getUserStackEventsBucketKey(
      String userId, String appId, String envId, StackType stackType) {
    return String.format(
        "users/%s/apps/%s/envs/%s/stacks/%s/events/%s",
        userId, appId, envId, stackType.getValue().toUpperCase(), STACK_EVENT_FILENAME);
  }

  public StackEventData getStackEvents(
      String orgId,
      String applicationId,
      String environmentId,
      StackType stackType,
      Instant from,
      Instant to,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    var org = organizationService.getById(orgId);

    String orgStackEventsBucketKey =
        getOrgStackEventsBucketKey(orgId, applicationId, environmentId, stackType);
    String userStackEventsBucketKey =
        getUserStackEventsBucketKey(org.getOwnerId(), applicationId, environmentId, stackType);

    Predicate<StackEvent> filterByInstantInterval =
        se -> {
          Instant timestamp = se.getTimestamp();
          assert timestamp != null;
          boolean isLessThanFrom =
              Optional.ofNullable(from).map(t -> lessThanOrEquals(t, timestamp)).orElse(true);
          boolean isGreaterThanTo =
              Optional.ofNullable(to).map(t -> greaterThanOrEquals(t, timestamp)).orElse(true);
          return isLessThanFrom && isGreaterThanTo;
        };

    var filteredStacks =
        getFilteredStackEventData(
            orgStackEventsBucketKey, filterByInstantInterval, pageFromOne, boundedPageSize);

    if (filteredStacks.stackData().data().isEmpty()) {
      return getFilteredStackEventData(
          userStackEventsBucketKey, filterByInstantInterval, pageFromOne, boundedPageSize);
    }

    return filteredStacks;
  }

  public Page<StackEvent> getStackEvents(
      String orgId,
      String applicationId,
      String environmentId,
      StackType stackType,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    var org = organizationService.getById(orgId);

    String orgStackEventsBucketKey =
        getOrgStackEventsBucketKey(orgId, applicationId, environmentId, stackType);

    String userStackEventsBucketKey =
        getUserStackEventsBucketKey(org.getOwnerId(), applicationId, environmentId, stackType);

    Predicate<StackEvent> noFilterPredicate = (stackEvent) -> true;

    var filteredStacks =
        getFilteredStackEventData(
                orgStackEventsBucketKey, noFilterPredicate, pageFromOne, boundedPageSize)
            .stackData();

    if (filteredStacks.data().isEmpty()) {
      return getFilteredStackEventData(
              userStackEventsBucketKey, noFilterPredicate, pageFromOne, boundedPageSize)
          .stackData();
    }

    return filteredStacks;
  }

  public List<StackEvent> crupdateStackEvents(String stackIdOrName, String bucketKey) {
    List<StackEvent> stackEvents =
        cloudformationComponent.getStackEvents(stackIdOrName).stream().map(mapper::toRest).toList();
    try {
      File stackEventJsonFile;
      if (bucketComponent.doesExist(bucketKey)) {
        stackEventJsonFile = bucketComponent.download(bucketKey);
        List<StackEvent> actual = om.readValue(stackEventJsonFile, new TypeReference<>() {});
        List<StackEvent> merged = mergeAndSortStackEventList(actual, stackEvents);
        om.writeValue(stackEventJsonFile, merged);
        bucketComponent.upload(stackEventJsonFile, bucketKey);
        return merged;
      } else {
        stackEventJsonFile = createTempFile("log", ".json");
        om.writeValue(stackEventJsonFile, stackEvents);
        bucketComponent.upload(stackEventJsonFile, bucketKey);
        return stackEvents;
      }
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  private StackEventData getFilteredStackEventData(
      String bucketKey,
      Predicate<StackEvent> filterPredicate,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    try {
      List<StackEvent> stackData =
          fromStackDataFileToList(bucketComponent, om, bucketKey, StackEvent.class);
      var paginatedData =
          paginator.apply(
              pageFromOne, boundedPageSize, stackData.stream().filter(filterPredicate).toList());
      return new StackEventData(
          paginatedData, stackData.isEmpty() ? null : stackData.getFirst().getTimestamp());
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  private static boolean greaterThanOrEquals(Instant instant1, Instant instant2) {
    return instant1.compareTo(instant2) >= 0;
  }

  private static boolean lessThanOrEquals(Instant instant1, Instant instant2) {
    return instant1.compareTo(instant2) <= 0;
  }

  private static <T> List<T> fromStackDataFileToList(
      ExtendedBucketComponent bucketComponent, ObjectMapper om, String bucketKey, Class<T> clazz)
      throws IOException {
    if (bucketComponent.doesExist(bucketKey)) {
      File stackDataFile = bucketComponent.download(bucketKey);
      return om.readValue(
          stackDataFile, om.getTypeFactory().constructCollectionType(List.class, clazz));
    }
    return List.of();
  }
}
