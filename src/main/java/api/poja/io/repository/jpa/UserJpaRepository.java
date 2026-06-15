package api.poja.io.repository.jpa;

import api.poja.io.model.UserStatisticsDTO;
import api.poja.io.model.UserStatus;
import api.poja.io.model.UserWithLatestOrgInviteDTO;
import api.poja.io.repository.model.SubscribedUserDTO;
import api.poja.io.repository.model.User;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<User, String> {
  @Query(
      " select new api.poja.io.repository.model.SubscribedUserDTO(u, us) from User u left"
          + " join UserSubscription us on u.id = us.userId  and "
          + " year(us.subscriptionBeginDatetime)=year(cast(?2 as date))and"
          + " month(us.subscriptionBeginDatetime)=month(cast(?2 as date))and"
          + " us.subscriptionBeginDatetime<=?2 and us.subscriptionEndDatetime>?2 where u.githubId ="
          + " ?1")
  Optional<SubscribedUserDTO> findByGithubId(String githubId, Instant referenceDatetime);

  boolean existsByEmail(String email);

  boolean existsByGithubId(String githubId);

  @Query(
      "select u from User u inner join OrganizationInvite invite on invite.invitedUser = u.id where"
          + " invite.inviterOrg = ?1 and invite.status = ?2 and invite.creationDatetime = (select"
          + " max(oInvite.creationDatetime) from OrganizationInvite oInvite"
          + " where oInvite.invitedUser = u.id and oInvite.inviterOrg = ?1)")
  Page<User> getAllUsersByOrgIdAndInviteStatus(
      String orgId, OrganizationInviteStatus status, Pageable pageable);

  @Query(
      "select u from User u inner join OrganizationInvite invite on invite.invitedUser = u.id where"
          + " invite.inviterOrg = ?1 and invite.status = ?2 and invite.creationDatetime = (select"
          + " max(oInvite.creationDatetime) from OrganizationInvite oInvite"
          + " where oInvite.invitedUser = u.id and oInvite.inviterOrg = ?1)")
  List<User> getAllUsersByOrgIdAndInviteStatus(String orgId, OrganizationInviteStatus status);

  @Query("update User u set u.mainOrgId = ?2 where u.id = ?1")
  @Modifying
  void updateMainOrgId(String userId, String mainOrgId);

  @Query("update User u set u.stripeId = ?2 where u.id = ?1")
  @Modifying
  void updateStripeId(String userId, String stripeId);

  @Query(
      " select new api.poja.io.repository.model.SubscribedUserDTO(u, us) from User u left"
          + " join UserSubscription us on u.id = us.userId  and "
          + " year(us.subscriptionBeginDatetime)=year(cast(?2 as date))and"
          + " month(us.subscriptionBeginDatetime)=month(cast(?2 as date))and"
          + " us.subscriptionBeginDatetime<=?2 and us.subscriptionEndDatetime>?2"
          + "where lower(u.id) like lower(concat('%',?1,'%'))")
  Page<SubscribedUserDTO> findByUsernameContainingIgnoreCase(
      String username, Instant referenceDatetime, Pageable pageable);

  @Query(
      value =
          """
            select u from User u where lower(u.id) like lower(concat('%',?1,'%'))
            order by
              case when ?2 = 'ASC' then u.username end asc,
              case when ?2 = 'DESC' then u.username end desc
          """)
  List<User> findByUsernameSortedByUsername(String username, String sortOrder, Pageable pageable);

  @Query(
      value =
          """
            select u from User u where lower(u.id) like lower(concat('%',?1,'%'))
            order by
              case when ?2 = 'ASC' then u.joinedAt end asc,
              case when ?2 = 'DESC' then u.joinedAt end desc
          """)
  List<User> findByUsernameSortedByJoinedAt(String username, String sortOrder, Pageable pageable);

  @Query(
      value =
          """
  select u from User u where lower(u.id) like lower(concat('%',?1,'%'))
  order by
    case when ?2 = 'ASC' then u.lastConnection end asc,
    case when ?2 = 'DESC' then u.lastConnection end desc
""")
  List<User> findByUsernameSortedByLastConnection(
      String username, String sortOrder, Pageable pageable);

  @Query(
      """
    select distinct new api.poja.io.model.UserWithLatestOrgInviteDTO (
        u.id, u.username, u.email, u.firstName, u.lastName, u.avatar, u.status, u.joinedAt, u.lastConnection, u.statusUpdatedAt, u.statusCheckedAt, u.statusReason, u.archived,us,
        (select count(o)
         from Organization o
         where exists (
             select 1
             from OrganizationInvite oInvite
             where oInvite.inviterOrg = o.id
               and oInvite.invitedUser = u.id
               and oInvite.status = 'ACCEPTED'
               and oInvite.creationDatetime = (
                   select max(oInvite2.creationDatetime)
                   from OrganizationInvite oInvite2
                   where oInvite2.inviterOrg = oInvite.inviterOrg
                     and oInvite2.invitedUser = oInvite.invitedUser
               )
         )
        ),
        oInvite,
        u.latestSubscriptionId
    )
    from User u
    left join OrganizationInvite oInvite on oInvite.invitedUser = u.id
    and oInvite.creationDatetime = (
        select max(oInvite2.creationDatetime)
        from OrganizationInvite oInvite2
        where oInvite2.invitedUser = u.id and oInvite2.inviterOrg = ?1
    )
    left join UserSubscription us on us.userId = u.id and year(us.subscriptionBeginDatetime)=year(cast(?3 as date))and
          month(us.subscriptionBeginDatetime)=month(cast(?3 as date))and
          us.subscriptionBeginDatetime<=?3 and us.subscriptionEndDatetime>?3
    where lower(u.username) like lower(concat('%', ?2, '%'))
""")
  Page<UserWithLatestOrgInviteDTO> findAllUsersByUsernameWithLatestOrgInvite(
      String orgId, String username, Instant referenceDatetime, Pageable pageable);

  @Modifying
  @Query(
      """
update User u set u.status = ?2, u.statusReason = ?3, u.statusUpdatedAt = ?4, u.statusCheckedAt = ?5 where u.id = ?1
""")
  void updateStatus(
      String userId,
      UserStatus status,
      String statusReason,
      Instant statusUpdatedAt,
      Instant statusCheckedAt);

  @Modifying
  @Query(
      """
      update User u set u.statusCheckedAt = ?2 where u.id = ?1
      """)
  void updateStatusCheckedAt(String userId, Instant now);

  @Modifying
  @Query(
      """
      update User u set u.archived = true, u.archivedAt = ?2 where u.id = ?1
      """)
  void archiveUser(String userId, Instant archivedAt);

  @Query(
      """
 select u from User u where (YEAR(u.joinedAt)*100 + MONTH(u.joinedAt) <= ?1) and (u.archived = false or (u.archived = true and YEAR(u.archivedAt)*100+MONTH(u.archivedAt) >= ?1))
""")
  List<User> findAllToBillFor(long yearMonth);

  @Query(
      """
select COUNT(u) > 0 from User u where u.id = ?1 and (YEAR(u.joinedAt)*100 + MONTH(u.joinedAt) <= ?2) and (u.archived = false or (u.archived = true and YEAR(u.archivedAt)*100+MONTH(u.archivedAt) >= ?2))
""")
  boolean shouldComputeCost(String userId, long yearMonth);

  @Query(
      """
  select u from User u where u.joinedAt<= ?1 and (u.archived = false or (u.archived = true and DATE(u.archivedAt) >= ?2))
""")
  List<User> findAllToComputeBilling(Instant computeDatetime, LocalDate dateIntervalEnd);

  // TODO: add mapping to subscription to all methods
  @Query(
      " select new api.poja.io.repository.model.SubscribedUserDTO(u, us) from User u left"
          + " join UserSubscription us on u.id = us.userId  and "
          + " year(us.subscriptionBeginDatetime)=year(cast(?2 as date))and"
          + " month(us.subscriptionBeginDatetime)=month(cast(?2 as date))and"
          + " us.subscriptionBeginDatetime<=?2 and us.subscriptionEndDatetime>?2 where u.id = ?1")
  Optional<SubscribedUserDTO> getUserDTOById(String id, Instant referenceDatetime);

  @Modifying
  @Query(
      """
      update User u set u.latestSubscriptionId = ?2 where u.id = ?1
      """)
  void updateLatestSubscriptionId(String userId, String subscriptionId);

  @Query(
      """
select new api.poja.io.model.UserStatisticsDTO(
  coalesce(count(u), 0L),
  coalesce(sum(case when u.archived = false and u.status = 'SUSPENDED' then 1 else 0 end), 0L),
  coalesce(sum(case when u.archived = true then 1 else 0 end), 0L)
) from User u
""")
  UserStatisticsDTO getUserStatisticsDTO();

  @Modifying
  @Query(
      """
      update User u set u.lastConnection = ?2 where u.id = ?1
      """)
  void updateLastConnection(String userId, Instant now);
}
