package api.poja.io.model.pojaConf.conf2;

import api.poja.io.endpoint.rest.model.ConcurrencyConf2;
import api.poja.io.model.pojaConf.ConcurrencyConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;

public class PojaConf2Concurrency implements ConcurrencyConfig {
  public static final PojaConf2Concurrency BASIC_USER_CONCURRENCY =
      new PojaConf2Concurrency(5, 5, 5);
  public static final PojaConf2Concurrency PREMIUM_USER_CONCURRENCY =
      new PojaConf2Concurrency(50, 50, 50);

  @Builder
  public PojaConf2Concurrency(ConcurrencyConf2 rest) {
    this(
        rest.getFrontalReservedConcurrentExecutionsNb(),
        rest.getWorker1ReservedConcurrentExecutionsNb(),
        rest.getWorker2ReservedConcurrentExecutionsNb());
  }

  @JsonProperty("frontal_reserved_concurrent_executions_nb")
  private final Integer frontalReservedConcurrency;

  @JsonProperty("worker_1_reserved_concurrent_executions_nb")
  private final Integer worker1ReservedConcurrency;

  @JsonProperty("worker_2_reserved_concurrent_executions_nb")
  private final Integer worker2ReservedConcurrency;

  public PojaConf2Concurrency(
      @JsonProperty("frontal_reserved_concurrent_executions_nb") Integer frontalReservedConcurrency,
      @JsonProperty("worker_1_reserved_concurrent_executions_nb")
          Integer worker1ReservedConcurrency,
      @JsonProperty("worker_2_reserved_concurrent_executions_nb")
          Integer worker2ReservedConcurrency) {
    this.frontalReservedConcurrency = frontalReservedConcurrency;
    this.worker1ReservedConcurrency = worker1ReservedConcurrency;
    this.worker2ReservedConcurrency = worker2ReservedConcurrency;
  }

  public ConcurrencyConf2 toRest() {
    return new ConcurrencyConf2()
        .frontalReservedConcurrentExecutionsNb(frontalReservedConcurrency)
        .worker1ReservedConcurrentExecutionsNb(worker1ReservedConcurrency)
        .worker2ReservedConcurrentExecutionsNb(worker2ReservedConcurrency);
  }

  public static Set<String> getBasicUserInvalidAttributes(ConcurrencyConf2 concurrency) {
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

    if (concurrency.getWorker1ReservedConcurrentExecutionsNb() != null) {
      if (concurrency.getWorker1ReservedConcurrentExecutionsNb()
          > basicMax.worker1ReservedConcurrency) {
        result.add(
            "worker1_reserved_concurrent_executions_nb cannot be greater than "
                + basicMax.worker1ReservedConcurrency);
      }
      if (concurrency.getWorker1ReservedConcurrentExecutionsNb() < 0) {
        result.add("worker1_reserved_concurrent_executions_nb cannot be less than 0");
      }
    }

    if (concurrency.getWorker2ReservedConcurrentExecutionsNb() != null) {
      if (concurrency.getWorker2ReservedConcurrentExecutionsNb()
          > basicMax.worker2ReservedConcurrency) {
        result.add(
            "worker1_reserved_concurrent_executions_nb cannot be greater than "
                + basicMax.worker2ReservedConcurrency);
      }
      if (concurrency.getWorker2ReservedConcurrentExecutionsNb() < 0) {
        result.add("worker2_reserved_concurrent_executions_nb cannot be less than 0");
      }
    }
    return result;
  }

  public static Set<String> getPremiumUserInvalidAttributes(ConcurrencyConf2 concurrency) {
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
    if (concurrency.getWorker1ReservedConcurrentExecutionsNb() != null) {
      if (concurrency.getWorker1ReservedConcurrentExecutionsNb()
          > premiumMax.worker1ReservedConcurrency) {
        result.add(
            "worker1_reserved_concurrent_executions_nb cannot be greater than "
                + premiumMax.worker1ReservedConcurrency);
      }

      if (concurrency.getWorker1ReservedConcurrentExecutionsNb() < 0) {
        result.add("worker1_reserved_concurrent_executions_nb cannot be less than 0");
      }
    }

    if (concurrency.getWorker2ReservedConcurrentExecutionsNb() != null) {
      if (concurrency.getWorker2ReservedConcurrentExecutionsNb()
          > premiumMax.worker2ReservedConcurrency) {
        result.add(
            "worker1_reserved_concurrent_executions_nb cannot be greater than "
                + premiumMax.worker2ReservedConcurrency);
      }
      if (concurrency.getWorker2ReservedConcurrentExecutionsNb() < 0) {
        result.add("worker2_reserved_concurrent_executions_nb cannot be less than 0");
      }
    }

    return result;
  }

  @JsonProperty("frontal_reserved_concurrent_executions_nb")
  public Integer frontalReservedConcurrency() {
    return frontalReservedConcurrency;
  }

  @JsonProperty("worker_1_reserved_concurrent_executions_nb")
  public Integer worker1ReservedConcurrency() {
    return worker1ReservedConcurrency;
  }

  @JsonProperty("worker_2_reserved_concurrent_executions_nb")
  public Integer worker2ReservedConcurrency() {
    return worker2ReservedConcurrency;
  }
}
