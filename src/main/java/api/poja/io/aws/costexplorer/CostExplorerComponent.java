package api.poja.io.aws.costexplorer;

import static java.math.BigDecimal.ZERO;
import static software.amazon.awssdk.services.costexplorer.model.Dimension.SERVICE;
import static software.amazon.awssdk.services.costexplorer.model.Granularity.MONTHLY;

import api.poja.io.model.DateInterval;
import api.poja.io.model.cost.CostByTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.Expression;
import software.amazon.awssdk.services.costexplorer.model.MetricValue;
import software.amazon.awssdk.services.costexplorer.model.TagValues;

@Slf4j
@AllArgsConstructor
@Component
public class CostExplorerComponent {

  private static final String UNBLENDED_COST_METRIC = "UnblendedCost";
  private static final String[] SERVICES_TO_BILL = {
    "Amazon Simple Storage Service", "Amazon Simple Queue Service", "AWS Lambda",
  };

  private final CostExplorerClient client;

  public CostByTime getMonthlyServiceCostsByTags(LocalDate date, TagValues... tags) {
    var timePeriod = timePeriod(date);
    var tagList = List.of(tags);

    log.info("Retrieving Unblended cost for timePeriod {} with tags {}", timePeriod, tags);

    var response =
        client.getCostAndUsage(
            req ->
                req.timePeriod(timePeriod.toAwsDateInterval())
                    .granularity(MONTHLY)
                    .metrics(UNBLENDED_COST_METRIC)
                    .filter(
                        Expression.builder().and(expr(tagList), expr(SERVICES_TO_BILL)).build()));

    var resultsByTime = response.resultsByTime();
    if (resultsByTime.isEmpty()) {
      return new CostByTime(timePeriod, ZERO);
    }

    if (resultsByTime.size() > 1) {
      log.info("Found more than one result for tags {}", tagList);
    }

    var totalMetric = resultsByTime.getFirst().total();

    if (!totalMetric.containsKey(UNBLENDED_COST_METRIC)) {
      log.error("No UnblendedCost found for tags {}", tagList);
      return new CostByTime(timePeriod, ZERO);
    }

    MetricValue unblendedCost = totalMetric.get(UNBLENDED_COST_METRIC);
    return new CostByTime(timePeriod, new BigDecimal(unblendedCost.amount()));
  }

  private static DateInterval timePeriod(LocalDate date) {
    var startDate = date.withDayOfMonth(1);
    var endDate = date.withDayOfMonth(date.lengthOfMonth()).plusDays(1); // end-exclusive
    return new DateInterval(startDate, endDate);
  }

  private static Expression expr(String... services) {
    return Expression.builder().dimensions(d -> d.key(SERVICE).values(services)).build();
  }

  private static Expression expr(List<TagValues> tags) {
    List<Expression> expressions =
        tags.stream()
            .map(t -> Expression.builder().tags(tag -> tag.key(t.key()).values(t.values())).build())
            .toList();
    if (expressions.size() > 1) {
      return Expression.builder().and(expressions).build();
    }
    return expressions.getFirst();
  }
}
