package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.endpoint.rest.model.LogQueryStatus;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "\"user_app_log_query\"")
@EqualsAndHashCode
@ToString
public class LogQuery {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @CreationTimestamp private Instant creationDatetime;

  @Type(StringArrayType.class)
  @Column(name = "filter_keywords", columnDefinition = "text[]")
  private String[] filterKeywords;

  public List<String> getFilterKeywordsAsList() {
    return Arrays.asList(filterKeywords);
  }

  public void setFilterKeywords(List<String> filterKeywords) {
    this.filterKeywords = filterKeywords.toArray(new String[0]);
  }

  private String userId;
  private String appId;
  private String queryId;
  private String orgId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private LogQueryStatus queryStatus;
}
