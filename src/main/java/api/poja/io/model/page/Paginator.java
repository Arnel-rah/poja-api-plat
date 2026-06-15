package api.poja.io.model.page;

import static java.lang.Math.min;

import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Paginator {
  public <T> Page<T> apply(PageFromOne pageFromOne, BoundedPageSize boundedPageSize, List<T> ts) {
    if (!ts.isEmpty()) {
      int firstIndex = (pageFromOne.getValue() - 1) * boundedPageSize.getValue();
      int lastIndex = min(firstIndex + boundedPageSize.getValue(), ts.size());
      var data = ts.subList(firstIndex, lastIndex);
      return new Page<>(pageFromOne, boundedPageSize, data);
    }
    return new Page<>(pageFromOne, boundedPageSize, ts);
  }
}
