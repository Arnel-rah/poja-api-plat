package api.poja.io.datastructure;

@FunctionalInterface
public interface BiParametricSupplier<A, B, T> {
  /**
   * Gets a result.
   *
   * @param a the argument
   * @param b the argument
   * @return a result
   */
  T get(A a, B b);
}
