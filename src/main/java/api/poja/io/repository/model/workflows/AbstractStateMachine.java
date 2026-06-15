package api.poja.io.repository.model.workflows;

import static java.util.stream.Collectors.joining;

import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import api.poja.io.repository.model.workflows.exception.InvalidStatusTransitionException;
import api.poja.io.repository.model.workflows.exception.OlderStateTransitionException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public abstract non-sealed class AbstractStateMachine<S extends Enum<S>, T extends State<S>>
    implements StateMachine<S, T> {

  /**
   * The subclass MUST MANAGE the states STORAGE
   *
   * <p>Returns a list containing the stored states
   *
   * @return a list containing the states
   */
  protected abstract List<T> internalStates();

  /**
   * The subclass MUST MANAGE the states STORAGE
   *
   * <p>Adds a new state
   */
  protected abstract void addState(T state);

  /**
   * Returns the result of transition attempt from a state, to another one
   *
   * @param from the current state
   * @param to the target state
   * @return the result of transition attempt
   */
  public abstract TransitionResult<S, T> canTransitionTo(T from, T to);

  @Override
  public final List<T> states() {
    return internalStates().stream()
        .sorted(Comparator.comparing(T::getTimestamp).reversed())
        .toList();
  }

  @Override
  public final Optional<T> currentState() {
    List<T> states = states();
    return states.isEmpty() ? Optional.empty() : Optional.of(states.getFirst());
  }

  @Override
  public final T transitionTo(T targetState) throws IllegalStateTransitionException {
    List<T> states = states();
    if (states.isEmpty()) {
      addState(targetState);
      return targetState;
    }
    var currentState = states.getFirst();
    if (targetState.getTimestamp().isBefore(currentState.getTimestamp())) {
      throw new OlderStateTransitionException(currentState, targetState);
    }
    var result = canTransitionTo(currentState, targetState);
    if (!result.ok()) {
      throw new InvalidStatusTransitionException(currentState, targetState);
    }
    var resolvedState = result.resolvedState();
    addState(resolvedState);
    return resolvedState;
  }

  @Override
  public String describe() {
    List<T> states = states();
    if (states.isEmpty()) return "";
    return states.stream()
        .map(s -> String.format("[%s]", s.getProgressionStatus()))
        .collect(joining(" -> "));
  }
}
