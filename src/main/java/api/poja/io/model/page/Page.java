package api.poja.io.model.page;

import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public record Page<T>(PageFromOne queryPage, BoundedPageSize queryPageSize, Collection<T> data) {
  public boolean hasPrevious() {
    return queryPage.getValue() > 1;
  }

  public int count() {
    return data.size();
  }

  public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
    List<R> mappedResult = data.stream().map(mapper).map(obj -> (R) obj).toList();
    return new Page<>(this.queryPage, this.queryPageSize, mappedResult);
  }

  public Page<T> filter(Predicate<? super T> predicate) {
    return new Page<>(this.queryPage, this.queryPageSize, data.stream().filter(predicate).toList());
  }
}
