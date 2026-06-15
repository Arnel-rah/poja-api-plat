package api.poja.io.repository.model.workflows;

/**
 * Represents the result of a state transition attempt in a state machine
 *
 * @param <S> the type of the {@code State} progression status used by this state machine
 * @param <T> the type of {@code State} used by this state machine
 * @see StateMachine
 */
public record TransitionResult<S extends Enum<S>, T extends State<S>>(boolean ok, T resolvedState) {
  public static <S extends Enum<S>, T extends State<S>> TransitionResult<S, T> ok(T resolvedState) {
    return new TransitionResult<>(true, resolvedState);
  }

  public static <S extends Enum<S>, T extends State<S>> TransitionResult<S, T> ko(T resolvedState) {
    return new TransitionResult<>(false, resolvedState);
  }

  public static <S extends Enum<S>, T extends State<S>> TransitionResult<S, T> ko() {
    return new TransitionResult<>(false, null);
  }
}
