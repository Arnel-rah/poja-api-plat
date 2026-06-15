package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.CreateLogQueryRequestBody;
import api.poja.io.endpoint.rest.model.CreateLogQueryResponse;
import api.poja.io.repository.model.LogQuery;
import org.springframework.stereotype.Component;

@Component
public class LogQueryMapper {
  public CreateLogQueryResponse toRest(LogQuery domain, CreateLogQueryRequestBody requestBody) {
    return new CreateLogQueryResponse()
        .id(domain.getId())
        .timestampSortDirection(requestBody.getTimestampSortDirection())
        .filterKeywords(domain.getFilterKeywordsAsList())
        .filterFrom(requestBody.getFilterFrom())
        .filterTo(requestBody.getFilterTo())
        .envTypes(requestBody.getEnvTypes())
        .functionTypes(requestBody.getFunctionTypes());
  }
}
