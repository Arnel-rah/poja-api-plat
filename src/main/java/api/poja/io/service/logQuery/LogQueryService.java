package api.poja.io.service.logQuery;

import static api.poja.io.endpoint.rest.model.LogQueryStatus.COMPLETED;
import static api.poja.io.endpoint.rest.model.LogQueryStatus.PENDING;
import static java.util.Objects.requireNonNull;

import api.poja.io.aws.cloudwatch.CloudwatchComponent;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.LogQueryCreated;
import api.poja.io.endpoint.rest.model.CreateLogQueryRequestBody;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.FunctionType;
import api.poja.io.endpoint.rest.model.LogQueryStatus;
import api.poja.io.endpoint.rest.model.PagedLogQueryResults;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.LogQueryRepository;
import api.poja.io.repository.model.LogQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetQueryResultsResponse;

@Service
@AllArgsConstructor
public class LogQueryService {
  private final LogQueryRepository repository;
  private final CloudwatchComponent cloudwatchComponent;
  private final EventProducer<LogQueryCreated> eventProducer;
  private final LogQueryMapper logQueryMapper;

  public LogQuery createNew(String orgId, String appId, CreateLogQueryRequestBody query) {
    LogQuery logQuery = LogQuery.builder().queryStatus(PENDING).appId(appId).orgId(orgId).build();
    logQuery.setFilterKeywords(requireNonNull(query.getFilterKeywords()));
    var saved = repository.save(logQuery);
    eventProducer.accept(
        List.of(
            LogQueryCreated.builder()
                .queryTimestampSortDirection(query.getTimestampSortDirection())
                .queryInstantIntervalEnd(query.getFilterTo())
                .queryInstantIntervalBegin(query.getFilterFrom())
                .environmentTypes(new HashSet<>(requireNonNull(query.getEnvTypes())))
                .queryFilterKeywords(new HashSet<>(query.getFilterKeywords()))
                .domainQueryId(saved.getId())
                .functionTypes(new HashSet<>(requireNonNull(query.getFunctionTypes())))
                .build()));
    return saved;
  }

  @Transactional
  public void updateQueryId(String id, String queryId) {
    repository.updateQueryId(id, queryId);
  }

  @Transactional
  public void updateQueryStatus(String id, LogQueryStatus status) {
    repository.updateQueryStatus(id, status);
  }

  public Optional<LogQuery> findById(String id) {
    return repository.findById(id);
  }

  public LogQuery getById(String id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("LogQuery.Id = " + id + " not found"));
  }

  public api.poja.io.endpoint.rest.model.PagedLogQuery restById(
      String id,
      PageFromOne page,
      BoundedPageSize pageSize,
      Set<EnvironmentType> environmentTypes,
      Set<FunctionType> functionTypes) {
    LogQuery domain = getById(id);
    if (!COMPLETED.equals(domain.getQueryStatus())) {
      return new api.poja.io.endpoint.rest.model.PagedLogQuery()
          .id(domain.getId())
          .filterKeywords(domain.getFilterKeywordsAsList())
          .queryStatus(domain.getQueryStatus())
          .hasResults(false)
          .results(new PagedLogQueryResults().data(List.of()));
    }
    GetQueryResultsResponse queryResultResponse =
        cloudwatchComponent.getQueryResult(domain.getQueryId());
    if (!queryResultResponse.hasResults()) {
      return new api.poja.io.endpoint.rest.model.PagedLogQuery()
          .id(domain.getId())
          .filterKeywords(domain.getFilterKeywordsAsList())
          .queryStatus(domain.getQueryStatus())
          .hasResults(false)
          .results(new PagedLogQueryResults().data(List.of()));
    }
    var results =
        logQueryMapper.mapResults(
            page, pageSize, environmentTypes, functionTypes, queryResultResponse);
    return new api.poja.io.endpoint.rest.model.PagedLogQuery()
        .id(domain.getId())
        .filterKeywords(domain.getFilterKeywordsAsList())
        .queryStatus(domain.getQueryStatus())
        .hasResults(results.count() > 0)
        .results(logQueryMapper.toRest(results));
  }
}
