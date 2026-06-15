package api.poja.io.datastructure;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ListUtils {
  public static <T> T getOrDefault(List<? extends T> list, int position, @Nullable T defaultValue) {
    if (list == null) {
      throw new NullPointerException();
    }
    if (position < 0) {
      throw new IndexOutOfBoundsException("position (" + position + ") must not be negative");
    }
    return position < list.size() ? list.get(position) : defaultValue;
  }

  public static <T> Optional<T> find(List<? extends T> list, int position) {
    try {
      var element = getOrDefault(list, position, null);
      return Optional.ofNullable(element);
    } catch (IndexOutOfBoundsException e) {
      return Optional.empty();
    }
  }
}
