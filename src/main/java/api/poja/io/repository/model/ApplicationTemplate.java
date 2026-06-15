package api.poja.io.repository.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"application_template\"")
@EqualsAndHashCode
@ToString
@Immutable
public class ApplicationTemplate {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String name;

  private String description;

  private String repositoryUrl;

  private String demoUrl;

  private boolean withCustomConfig;
}
