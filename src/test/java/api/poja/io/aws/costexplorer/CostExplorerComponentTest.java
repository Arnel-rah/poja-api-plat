package api.poja.io.aws.costexplorer;

import static software.amazon.awssdk.regions.Region.EU_WEST_3;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.TagValues;

@Slf4j
@Disabled
class CostExplorerComponentTest {

  static final List<TagValues> FILTER_TAGS =
      List.of(TagValues.builder().key("user:poja").values("haapi", "poja-api").build());
  static final LocalDate DATE = LocalDate.now();

  // Requires the 'AWS_SECRET_ACCESS_KEY' and 'AWS_ACCESS_KEY_ID' env vars to be set
  final CostExplorerComponent costExplorer =
      new CostExplorerComponent(CostExplorerClient.builder().region(EU_WEST_3).build());

  @Test
  void getMonthlyServiceCostsByTags() {

    var costByTime =
        costExplorer.getMonthlyServiceCostsByTags(DATE, FILTER_TAGS.toArray(new TagValues[0]));

    log.info("costByTime {} {}", costByTime.timePeriod(), costByTime.amount());
  }
}
