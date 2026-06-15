package api.poja.io.repository.jpa;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.*;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.repository.model.Application;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
class ApplicationRepositoryTest extends MockedThirdParties {
  @Qualifier("ApplicationRepository")
  @Autowired
  ApplicationRepository subject;

  @PersistenceContext EntityManager entityManager;

  @Test
  @Transactional
  void findAllToComputeBillingForByOrgId() {
    String appId = "archived_poja_application_id";
    var archived = subject.findById(appId).get();
    var refDate = LocalDate.parse("2020-01-01");
    Instant refInstant = refDate.atStartOfDay(UTC).toInstant();
    var updatedArchivedAt = updateDates(archived, refInstant, refInstant);

    var oneDayAgo =
        findAllToComputeBillingForByOrgIdWithGrace(
            archived.getOrgId(), refInstant, refDate.minusDays(1));
    var refDay =
        findAllToComputeBillingForByOrgIdWithGrace(archived.getOrgId(), refInstant, refDate);
    var afterOneDay =
        findAllToComputeBillingForByOrgIdWithGrace(
            archived.getOrgId(), refInstant, refDate.plusDays(1));
    var afterTwoDays =
        findAllToComputeBillingForByOrgIdWithGrace(
            archived.getOrgId(), refInstant, refDate.plusDays(2));

    assertEquals(
        updatedArchivedAt, oneDayAgo.stream().filter(a -> a.getId().equals(appId)).findAny().get());
    assertTrue(oneDayAgo.contains(updatedArchivedAt));
    assertTrue(refDay.contains(updatedArchivedAt));
    assertTrue(afterOneDay.contains(updatedArchivedAt));
    assertFalse(afterTwoDays.contains(updatedArchivedAt));
    // reset
    updateDates(archived, archived.getArchivedAt(), archived.getCreationDatetime());
  }

  List<Application> findAllToComputeBillingForByOrgIdWithGrace(
      String orgId, Instant computeDatetime, LocalDate date) {
    return subject.findAllToComputeBillingForByOrgId(orgId, computeDatetime, date.minusDays(1));
  }

  @Test
  @Transactional
  void findAllToBillByUserId() {
    String appId = "archived_poja_application_id";
    var archived = subject.findById(appId).get();
    YearMonth yearMonth = YearMonth.of(2020, 1);
    var creationDatetime = yearMonth.atDay(1).atStartOfDay(UTC).toInstant();
    var archivedAt = yearMonth.plusMonths(1).atDay(1).atStartOfDay(UTC).toInstant();
    var updatedArchivedAt = updateDates(archived, archivedAt, creationDatetime);

    var oneMonthAgoYM = yearMonth.minusMonths(1);
    var oneMonthAgo =
        subject.findAllToBillByUserId(
            archived.getUserId(), oneMonthAgoYM.getYear() * 100L + oneMonthAgoYM.getMonthValue());
    var thisMonth =
        subject.findAllToBillByUserId(
            archived.getUserId(), yearMonth.getYear() * 100L + yearMonth.getMonthValue());
    var nextMonthAgoYM = yearMonth.plusMonths(1);
    var nextMonth =
        subject.findAllToBillByUserId(
            archived.getUserId(), nextMonthAgoYM.getYear() * 100L + nextMonthAgoYM.getMonthValue());

    assertFalse(oneMonthAgo.contains(updatedArchivedAt));
    assertTrue(thisMonth.contains(updatedArchivedAt));
    assertTrue(nextMonth.contains(updatedArchivedAt));
    // reset
    updateDates(archived, archived.getArchivedAt(), archived.getCreationDatetime());
  }

  Application updateDates(Application app, Instant archivedAt, Instant creationDatetime) {
    entityManager
        .createQuery(
            "UPDATE Application a SET a.archivedAt = :archivedAt, a.creationDatetime ="
                + " :creationDatetime WHERE a.id = :id")
        .setParameter("archivedAt", archivedAt)
        .setParameter("creationDatetime", creationDatetime)
        .setParameter("id", app.getId())
        .executeUpdate();
    entityManager.flush();
    entityManager.clear();
    return app.toBuilder().creationDatetime(creationDatetime).archivedAt(archivedAt).build();
  }
}
