package api.poja.io.model;

import api.poja.io.endpoint.rest.model.StackEvent;
import api.poja.io.model.page.Page;
import java.time.Instant;

public record StackEventData(Page<StackEvent> stackData, Instant latestStackEventTimestamp) {}
