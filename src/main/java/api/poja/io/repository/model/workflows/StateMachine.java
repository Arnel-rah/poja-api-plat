package api.poja.io.repository.model.workflows;

import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @param <S> the type of the {@code State} progression status used by this state machine
 * @param <T> the type of {@code State} used by this state machine
 * @see AbstractStateMachine
 */
@Component
public sealed interface StateMachine<S extends Enum<S>, T extends State<S>>
    permits AbstractStateMachine {

  /**
   * Returns a list containing the StateMachine states
   *
   * @return a list containing the StateMachine states
   */
  List<T> states();

  /**
   * Returns an {@code Optional} describing the first element of this stream, or an empty {@code
   * Optional} if the stream is empty
   *
   * @return an {@code Optional} describing the first element of this stream, or an empty {@code
   *     Optional} if the stream is empty
   */
  Optional<T> currentState();

  /**
   * Returns the current state after the transition
   *
   * @return the current state after the transition
   * @throws IllegalStateTransitionException if the targetState is older than the ({@link
   *     StateMachine#currentState}) or if the
   */
  T transitionTo(T targetState) throws IllegalStateTransitionException;

  /**
   * Returns the result of transition attempt from a state, to another one
   *
   * @param from the current state
   * @param to the target state
   * @return the result of transition attempt
   */
  TransitionResult<S, T> canTransitionTo(T from, T to);

  /**
   * Returns the description of the current states
   *
   * @return the description of the current state
   */
  String describe();
}
