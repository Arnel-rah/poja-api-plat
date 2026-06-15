package api.poja.io.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class WorkerFunction {
  @Column(name = "worker_function_name")
  private String name;

  @Column(name = "worker_function_reserved_concurrency")
  private Integer workerFunctionReservedConcurrency;

  @Column(name = "worker_function_deleted", updatable = false)
  private boolean deleted;
}
