package api.poja.io.datastructure;

public record Pair<A, B>(A first, B second) {
  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<>(a, b);
  }
}
