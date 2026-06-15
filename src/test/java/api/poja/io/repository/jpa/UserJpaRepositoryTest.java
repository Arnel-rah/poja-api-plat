package api.poja.io.repository.jpa;

import static api.poja.io.endpoint.rest.security.model.UserRole.USER;
import static api.poja.io.model.UserStatus.ACTIVE;
import static api.poja.io.service.pricing.PricingMethod.TEN_MICRO;
import static java.time.Month.FEBRUARY;
import static java.time.Month.MARCH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.security.model.UserRole;
import api.poja.io.repository.model.User;
import java.time.Instant;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class UserJpaRepositoryTest extends MockedThirdParties {
  @Autowired UserJpaRepository subject;

  @Test
  void find_all_to_bill_for_ok() {
    var march2025 = 202503; // int version of 202503
    var april2025 = 202504; // int version of 202504
    var activeUsersInMarch2025 = subject.findAllToBillFor(march2025);
    var activeUsersInApril2025 = subject.findAllToBillFor(april2025);

    assertTrue(activeUsersInMarch2025.contains(archivedUser()));
    assertFalse(activeUsersInApril2025.contains(archivedUser()));
  }

  @Test
  void find_all_to_compute_billing_ok() {
    var at31dec2024 = Instant.parse("2024-12-31T00:00:00Z");
    var feb2025 = LocalDate.of(2025, FEBRUARY, 1);
    var march2025 = LocalDate.of(2025, MARCH, 26);

    var usersToComputeBillingFromDecToFeb = subject.findAllToComputeBilling(at31dec2024, feb2025);
    var usersToComputeBillingFromDecToMarch =
        subject.findAllToComputeBilling(at31dec2024, march2025);

    assertTrue(usersToComputeBillingFromDecToFeb.contains(archivedUser()));
    assertFalse(usersToComputeBillingFromDecToMarch.contains(archivedUser()));
  }

  private static User archivedUser() {
    return User.builder()
        .id("archived_id")
        .firstName("ar")
        .lastName("chived")
        .username("Archived")
        .email("archived@email.com")
        .roles(new UserRole[] {USER})
        .githubId("1009")
        .avatar("https://github.com/images/Archived.png")
        .pricingMethod(TEN_MICRO)
        .stripeId("archived_stripe_id")
        .joinedAt(Instant.parse("2024-03-25T12:00:00.00Z"))
        .archived(true)
        .status(ACTIVE)
        .mainOrgId("org-Archived-id")
        .archivedAt(Instant.parse("2025-03-25T12:00:00.00Z"))
        .statusUpdatedAt(Instant.parse("2025-03-25T12:00:00.00Z"))
        .lastConnection(Instant.parse("2025-10-01T14:00:00.00Z"))
        .build();
  }
}
