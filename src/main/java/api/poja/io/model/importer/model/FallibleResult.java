package api.poja.io.model.importer.model;

import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class FallibleResult<T, W, E> {
  @Nullable // null if !errors.isEmpty()
  protected final T value;
  protected final List<W> warnings;
  protected final List<E> errors;

  public boolean isSuccess() {
    return errors.isEmpty();
  }

  public static <T> FallibleResult<T, /*warning*/ Object, /*catch all*/ Exception> ofFallible(
      Callable<T> callable) {
    try {
      var result = callable.call();
      return new FallibleResult<>(result, List.of(), List.of());
    } catch (Exception e) {
      return new FallibleResult<>(null, List.of(), List.of(e));
    }
  }
}
