package api.poja.io.model.pojaConf.conf1;

import api.poja.io.endpoint.rest.model.ConcurrencyConf;
import api.poja.io.model.pojaConf.ConcurrencyConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;

public class PojaConf1Concurrency implements ConcurrencyConfig {
  @JsonProperty("frontal_reserved_concurrent_executions_nb")
  private final Integer frontalReservedConcurrency;

  @JsonProperty("worker_reserved_concurrent_executions_nb")
  private final Integer workerReservedConcurrency;

  private PojaConf1Concurrency(
      @JsonProperty("frontal_reserved_concurrent_executions_nb") Integer frontalReservedConcurrency,
      @JsonProperty("worker_reserved_concurrent_executions_nb") Integer workerReservedConcurrency) {
    this.frontalReservedConcurrency = frontalReservedConcurrency;
    this.workerReservedConcurrency = workerReservedConcurrency;
  }

  public static final PojaConf1Concurrency BASIC_USER_CONCURRENCY = new PojaConf1Concurrency(5, 5);
  public static final PojaConf1Concurrency PREMIUM_USER_CONCURRENCY =
      new PojaConf1Concurrency(50, 50);

  public ConcurrencyConf toRest() {
    return new ConcurrencyConf()
        .frontalReservedConcurrentExecutionsNb(frontalReservedConcurrency)
        .workerReservedConcurrentExecutionsNb(workerReservedConcurrency);
  }

  public static PojaConf1Concurrency fromRest(ConcurrencyConf concurrency) {
    return new PojaConf1Concurrency(
        concurrency.getFrontalReservedConcurrentExecutionsNb(),
        concurrency.getWorkerReservedConcurrentExecutionsNb());
  }

  public static Set<String> getBasicUserInvalidAttributes(ConcurrencyConf concurrency) {
    if (concurrency == null) {
      return Set.of("concurrency is mandatory");
    }
    Set<String> result = new HashSet<>();
    var basicMax = BASIC_USER_CONCURRENCY;
    if (concurrency.getFrontalReservedConcurrentExecutionsNb()
        > basicMax.frontalReservedConcurrency) {
      result.add(
          "frontal_reserved_concurrent_executions_nb cannot be greater than "
              + basicMax.frontalReservedConcurrency);
    }
    if (concurrency.getFrontalReservedConcurrentExecutionsNb() < 0) {
      result.add("frontal_reserved_concurrent_executions_nb cannot be less than 0");
    }
    if (concurrency.getWorkerReservedConcurrentExecutionsNb()
        > basicMax.workerReservedConcurrency) {
      result.add(
          "worker_reserved_concurrent_executions_nb cannot be greater than "
              + basicMax.workerReservedConcurrency);
    }
    if (concurrency.getWorkerReservedConcurrentExecutionsNb() < 0) {
      result.add("worker_reserved_concurrent_executions_nb cannot be less than 0");
    }
    return result;
  }

  public static Set<String> getPremiumUserInvalidAttributes(ConcurrencyConf concurrency) {
    if (concurrency == null) {
      return Set.of("concurrency is mandatory");
    }
    Set<String> result = new HashSet<>();
    var premiumMax = PREMIUM_USER_CONCURRENCY;
    if (concurrency.getFrontalReservedConcurrentExecutionsNb()
        > premiumMax.frontalReservedConcurrency) {
      result.add(
          "frontal_reserved_concurrent_executions_nb cannot be greater than "
              + premiumMax.frontalReservedConcurrency);
    }
    if (concurrency.getFrontalReservedConcurrentExecutionsNb() < 0) {
      result.add("frontal_reserved_concurrent_executions_nb cannot be less than 0");
    }
    if (concurrency.getWorkerReservedConcurrentExecutionsNb()
        > premiumMax.workerReservedConcurrency) {
      result.add(
          "worker_reserved_concurrent_executions_nb cannot be greater than "
              + premiumMax.workerReservedConcurrency);
    }
    if (concurrency.getWorkerReservedConcurrentExecutionsNb() < 0) {
      result.add("worker_reserved_concurrent_executions_nb cannot be less than 0");
    }
    return result;
  }

  @JsonProperty("frontal_reserved_concurrent_executions_nb")
  public Integer frontalReservedConcurrency() {
    return frontalReservedConcurrency;
  }

  @JsonProperty("worker_reserved_concurrent_executions_nb")
  public Integer workerReservedConcurrency() {
    return workerReservedConcurrency;
  }
}
