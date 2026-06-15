package api.poja.io.repository.jpa;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.*;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.repository.model.Environment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class EnvironmentRepositoryTest extends MockedThirdParties {
  @Autowired EnvironmentRepository subject;
  @PersistenceContext EntityManager em;

  @Test
  @Transactional
  void findAllEnvsToComputeBillingForByApplicationId() {
    Environment archived = subject.findById("archived_other_poja_app_env_id").get();
    String otherPojaApplicationId = archived.getApplicationId();
    var refDate = LocalDate.parse("2020-01-01");
    Instant refInstant = refDate.atStartOfDay(UTC).toInstant();
    var updatedArchivedAt = updateDates(archived, refInstant, refInstant);

    var oneDayAgo =
        findAllEnvsToComputeBillingForByApplicationIdWithGrace(
            otherPojaApplicationId, refInstant, refDate.minusDays(1));
    var refDay =
        findAllEnvsToComputeBillingForByApplicationIdWithGrace(
            otherPojaApplicationId, refInstant, refDate);
    var afterOneDay =
        findAllEnvsToComputeBillingForByApplicationIdWithGrace(
            otherPojaApplicationId, refInstant, refDate.plusDays(1));
    var afterTwoDays =
        findAllEnvsToComputeBillingForByApplicationIdWithGrace(
            otherPojaApplicationId, refInstant, refDate.plusDays(2));

    assertTrue(oneDayAgo.contains(updatedArchivedAt));
    assertTrue(refDay.contains(updatedArchivedAt));
    assertTrue(afterOneDay.contains(updatedArchivedAt));
    assertFalse(afterTwoDays.contains(updatedArchivedAt));
    // reset
    subject.save(archived);
  }

  List<Environment> findAllEnvsToComputeBillingForByApplicationIdWithGrace(
      String appId, Instant computeDatetime, LocalDate date) {
    return subject.findAllEnvsToComputeBillingForByApplicationId(
        appId, computeDatetime, date.minusDays(1));
  }

  Environment updateDates(Environment env, Instant archivedAt, Instant creationDatetime) {
    em.createQuery(
            "UPDATE Environment e SET e.archivedAt = :archivedAt, e.creationDatetime ="
                + " :creationDatetime WHERE e.id = :id")
        .setParameter("archivedAt", archivedAt)
        .setParameter("creationDatetime", creationDatetime)
        .setParameter("id", env.getId())
        .executeUpdate();
    em.flush();
    em.clear();
    return env.toBuilder().creationDatetime(creationDatetime).archivedAt(archivedAt).build();
  }
}
