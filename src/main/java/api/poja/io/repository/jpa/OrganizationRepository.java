package api.poja.io.repository.jpa;

import api.poja.io.model.OrganizationDTO;
import api.poja.io.repository.model.Organization;
import api.poja.io.repository.model.OrganizationInvite;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRepository extends JpaRepository<Organization, String> {

  @Query(
      """
    select new api.poja.io.model.OrganizationDTO(
        o.id, o.name, o.creationDatetime, o.ownerId,
        (select count(distinct u.id)
         from User u
         inner join OrganizationInvite oInvite
            on oInvite.invitedUser = u.id
         where
            oInvite.creationDatetime = (
                select max(oInvite2.creationDatetime)
                from OrganizationInvite oInvite2
                where oInvite2.invitedUser = u.id
                and oInvite2.inviterOrg = o.id
            )
            and oInvite.status = 'ACCEPTED')
    )
    from Organization o
    inner join OrganizationInvite oInvite
        on oInvite.inviterOrg = o.id
    where oInvite.invitedUser = ?1 and oInvite.status = ?2
    order by o.creationDatetime
""")
  Page<OrganizationDTO> findAllOrganizationsByUserIdAndStatus(
      String userId, OrganizationInviteStatus status, Pageable pageable);

  @Query(
      """
    select new api.poja.io.model.OrganizationDTO(
        o.id, o.name, o.creationDatetime, o.ownerId,
        (select count(distinct u.id)
         from User u
         inner join OrganizationInvite oInvite
            on oInvite.invitedUser = u.id
         where
            oInvite.creationDatetime = (
                select max(oInvite2.creationDatetime)
                from OrganizationInvite oInvite2
                where oInvite2.invitedUser = u.id
                and oInvite2.inviterOrg = o.id
            )
            and (oInvite.status = 'ACCEPTED' or oInvite.status = 'PENDING')
         )
    )
    from Organization o
    where o.id = ?1
""")
  Optional<OrganizationDTO> findByIdWithMembersCount(String id);

  @Query(
      "update Organization o set o.consoleUserGroupName = ?2, o.consoleUserGroupPolicyDocumentName"
          + " = ?3 where o.id = ?1")
  @Modifying
  void updateConsoleInformations(String orgId, String groupName, String policyDocumentName);

  List<Organization> findAllByOwnerId(String userId);

  long countByOwnerId(String userId);

  // TODO(perf-improvement): Could be added to the SQL.
  @Query(
      """
    select count(distinct u.id)
    from User u
    inner join OrganizationInvite oInvite
        on oInvite.invitedUser = u.id
    where
        oInvite.creationDatetime = (
            select max(oInvite2.creationDatetime)
            from OrganizationInvite oInvite2
            where oInvite2.invitedUser = u.id and oInvite2.inviterOrg = ?1
        )
        and (oInvite.status = 'ACCEPTED' or oInvite.status = 'PENDING')
""")
  long countAcceptedAndPendingMembersCountByOrgId(String orgId);

  @Query(
      """
    SELECT COUNT(o)
    FROM Organization o
    WHERE EXISTS (
        SELECT 1
        FROM OrganizationInvite oInvite
        WHERE oInvite.inviterOrg = o.id
          AND oInvite.invitedUser = ?1
          AND oInvite.status = 'ACCEPTED'
          AND oInvite.creationDatetime = (
              SELECT MAX(oInvite2.creationDatetime)
              FROM OrganizationInvite oInvite2
              WHERE oInvite2.inviterOrg = oInvite.inviterOrg
                AND oInvite2.invitedUser = oInvite.invitedUser
          )
    )

""")
  long countOrgMembershipsByUserId(String userId);

  @Query(
      """
      SELECT oInvite FROM OrganizationInvite oInvite
      WHERE oInvite.inviterOrg = ?1
      AND oInvite.creationDatetime = (
          SELECT MAX(oInvite2.creationDatetime) FROM OrganizationInvite oInvite2
          WHERE oInvite2.invitedUser = oInvite.invitedUser AND oInvite2.inviterOrg = ?1
      )
      AND oInvite.status = ?2
      """)
  Page<OrganizationInvite> findLatestInvitesByOrgIdAndStatus(
      String orgId, OrganizationInviteStatus status, Pageable pageable);

  @Modifying
  @Query(
      "update Organization o set o.consoleAccountId = ?2, o.consoleUsername = ?3, o.consolePassword"
          + " = ?4, o.consoleUserPolicyDocumentName = ?5 where o.id = ?1")
  void updateConsoleCredentials(
      String orgId,
      String consoleAccountId,
      String consoleUsername,
      String consolePassword,
      String consoleUserPolicyDocumentName);

  List<Organization> getAllByOwnerId(String ownerId);
}
