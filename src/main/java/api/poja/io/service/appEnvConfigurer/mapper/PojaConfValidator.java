package api.poja.io.service.appEnvConfigurer.mapper;

import api.poja.io.model.pojaConf.PojaConf;
import jakarta.annotation.Nullable;
import java.util.Set;
import org.apache.commons.lang3.function.TriConsumer;

/**
 * validates migration between two configurations, No output if from is null
 *
 * @param <T>
 */
public interface PojaConfValidator<T extends PojaConf> extends TriConsumer<T, T, Boolean> {
  // TODO: support upgrades
  Set<String> outputIllegalTransitions(@Nullable T from, T to, Boolean isPremium);
}
