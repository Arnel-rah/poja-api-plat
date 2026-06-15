package api.poja.io.repository.jpa;

import api.poja.io.repository.model.UserSubscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {
  List<UserSubscription> findAllByUserIdOrderBySubscriptionBeginDatetimeDesc(String userId);

  @Query(
      """
      select u from UserSubscription u
      where u.userId = ?1
       and u.invoice.status = 'PAID'
       and month(u.subscriptionBeginDatetime) = month(cast(?2 as date))
       and year(u.subscriptionBeginDatetime) = year(cast(?2 as date))
       and u.subscriptionBeginDatetime <= ?2
       and (u.subscriptionEndDatetime is null
       or (u.subscriptionEndDatetime is not null and u.subscriptionEndDatetime > ?2))
       order by u.subscriptionBeginDatetime desc
      """)
  List<UserSubscription> findAllActiveByUserId(String userId, Instant now);

  @Query(
      """
select u from UserSubscription u where u.userId = ?1 and u.subscriptionBeginDatetime > ?2 and (u.invoice.status <> 'PAID' or (u.subscriptionEndDatetime is not null and u.subscriptionEndDatetime <= ?2)) order by u.subscriptionBeginDatetime desc
""")
  List<UserSubscription> findAllNotActiveByUserId(String userId, Instant now);

  Optional<UserSubscription> findByUserIdAndId(String userId, String id);

  @Query(
      """
select exists(
select 1 from UserSubscription u
where u.userId = ?1
and month(u.subscriptionBeginDatetime) = month(cast(?2 as date))
and year(u.subscriptionBeginDatetime) = year(cast(?2 as date))
and u.subscriptionBeginDatetime <= ?2
and (u.invoice.status in ('PAID','PROCESSING','DRAFT','OPEN','UNKNOWN'))
and ((u.subscriptionEndDatetime is null) or (u.subscriptionEndDatetime is not null and u.subscriptionEndDatetime >?2))
)
""")
  boolean existsActiveOrUndergoingPaymentByUserId(String userId, Instant now);

  @Query(
      "select u from UserSubscription u where u.userId = ?1 and u.invoice.status = 'PAID' "
          + "and u.subscriptionBeginDatetime <= ?2"
          + "and month(u.subscriptionBeginDatetime) = month(cast(?2 as date))"
          + "and year(u.subscriptionBeginDatetime) = year(cast(?2 as date))"
          + "and ((u.subscriptionEndDatetime is null) or (u.subscriptionEndDatetime is not null "
          + "and u.subscriptionEndDatetime >?2))")
  Optional<UserSubscription> findActiveByUserId(String userId, Instant now);

  @Modifying
  @Query(
      """
update UserSubscription u set u.subscriptionEndDatetime = ?3 , u.willRenew = false where u.userId = ?1 and u.id = ?2
""")
  void updateSubscriptionEndFields(String userId, String id, Instant endDatetime);

  @Query(
      """
      select u from UserSubscription u
      where u.willRenew = true
      and year(u.subscriptionBeginDatetime) *100+month(u.subscriptionBeginDatetime)=?1
      and u.invoice.status = 'PAID'
      """)
  List<UserSubscription> findAllToRenew(long yearMonth);

  @Query(
      "select u from UserSubscription u where u.userId = ?1 and"
          + " year(u.subscriptionBeginDatetime)*100+month(u.subscriptionBeginDatetime) = ?2 and"
          + " u.invoice.status in ('REQUIRES_ACTION', 'REQUIRES_PAYMENT_METHOD',"
          + " 'REQUIRES_CAPTURE', 'REQUIRES_CONFIRMATION')")
  Optional<UserSubscription> findUnpaidOfYearMonth(String userId, long yearMonth);

  @Modifying
  @Query("update UserSubscription u set u.willRenew = ?2 where u.id = ?1")
  void updateWillRenew(String subscriptionId, boolean willRenew);
}
