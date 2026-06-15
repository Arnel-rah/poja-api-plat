package api.poja.io.model.pojaConf.conf9;

import api.poja.io.endpoint.rest.model.ComputeConf9;
import api.poja.io.endpoint.rest.model.ConcurrencyConf9;
import api.poja.io.model.pojaConf.ConcurrencyConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;

public class PojaConf9Concurrency implements ConcurrencyConfig {
  public static final PojaConf9Concurrency BASIC_USER_CONCURRENCY = new PojaConf9Concurrency(5, 5);
  public static final PojaConf9Concurrency PREMIUM_USER_CONCURRENCY =
      new PojaConf9Concurrency(50, 50);

  @JsonProperty("frontal_reserved_concurrent_executions_nb")
  private final Integer frontalReservedConcurrency;

  private final Integer workerReservedConcurrency;

  @Builder
  public PojaConf9Concurrency(ConcurrencyConf9 rest) {
    this(rest.getFrontalReservedConcurrentExecutionsNb(), null);
  }

  public PojaConf9Concurrency(
      @JsonProperty("frontal_reserved_concurrent_executions_nb") Integer frontalReservedConcurrency,
      Integer workerReservedConcurrency) {
    this.frontalReservedConcurrency = frontalReservedConcurrency;
    this.workerReservedConcurrency = workerReservedConcurrency;
  }

  public ConcurrencyConf9 toRest() {
    return new ConcurrencyConf9().frontalReservedConcurrentExecutionsNb(frontalReservedConcurrency);
  }

  public static Set<String> getBasicUserInvalidAttributes(
      ConcurrencyConf9 concurrency, ComputeConf9 compute) {
    if (concurrency == null) {
      return Set.of("concurrency is mandatory");
    }
    Set<String> result = new HashSet<>();
    var basicMax = BASIC_USER_CONCURRENCY;
    if (concurrency.getFrontalReservedConcurrentExecutionsNb() != null) {
      if (concurrency.getFrontalReservedConcurrentExecutionsNb()
          > basicMax.frontalReservedConcurrency) {
        result.add(
            "frontal_reserved_concurrent_executions_nb cannot be greater than "
                + basicMax.frontalReservedConcurrency);
      }
      if (concurrency.getFrontalReservedConcurrentExecutionsNb() < 0) {
        result.add("frontal_reserved_concurrent_executions_nb cannot be less than 0");
      }
    }
    if (compute != null && compute.getWorkers() != null) {
      var exceeded =
          compute.getWorkers().stream()
              .filter(w -> w.getReservedConcurrentExecutionsNb() != null)
              .filter(
                  w -> w.getReservedConcurrentExecutionsNb() > basicMax.workerReservedConcurrency)
              .findFirst();
      if (exceeded.isPresent()) {
        result.add(
            "workers[].reserved_concurrent_executions_nb cannot be greater than "
                + basicMax.workerReservedConcurrency);
      }
      var negative =
          compute.getWorkers().stream()
              .filter(w -> w.getReservedConcurrentExecutionsNb() != null)
              .filter(w -> w.getReservedConcurrentExecutionsNb() < 0)
              .findFirst();
      if (negative.isPresent()) {
        result.add("workers[].reserved_concurrent_executions_nb cannot be less than 0");
      }
    }
    return result;
  }

  public static Set<String> getPremiumUserInvalidAttributes(
      ConcurrencyConf9 concurrency, ComputeConf9 compute) {
    if (concurrency == null) {
      return Set.of("concurrency is mandatory");
    }
    Set<String> result = new HashSet<>();
    var premiumMax = PREMIUM_USER_CONCURRENCY;
    if (concurrency.getFrontalReservedConcurrentExecutionsNb() != null) {
      if (concurrency.getFrontalReservedConcurrentExecutionsNb()
          > premiumMax.frontalReservedConcurrency) {
        result.add(
            "frontal_reserved_concurrent_executions_nb cannot be greater than "
                + premiumMax.frontalReservedConcurrency);
      }
      if (concurrency.getFrontalReservedConcurrentExecutionsNb() < 0) {
        result.add("frontal_reserved_concurrent_executions_nb cannot be less than 0");
      }
    }
    if (compute != null && compute.getWorkers() != null) {
      var exceeded =
          compute.getWorkers().stream()
              .filter(w -> w.getReservedConcurrentExecutionsNb() != null)
              .filter(
                  w -> w.getReservedConcurrentExecutionsNb() > premiumMax.workerReservedConcurrency)
              .findFirst();
      if (exceeded.isPresent()) {
        result.add(
            "workers[].reserved_concurrent_executions_nb cannot be greater than "
                + premiumMax.workerReservedConcurrency);
      }
      var negative =
          compute.getWorkers().stream()
              .filter(w -> w.getReservedConcurrentExecutionsNb() != null)
              .filter(w -> w.getReservedConcurrentExecutionsNb() < 0)
              .findFirst();
      if (negative.isPresent()) {
        result.add("workers[].reserved_concurrent_executions_nb cannot be less than 0");
      }
    }
    return result;
  }

  @JsonProperty("frontal_reserved_concurrent_executions_nb")
  public Integer frontalReservedConcurrency() {
    return frontalReservedConcurrency;
  }
}
