package api.poja.io.repository.jpa;

import api.poja.io.repository.model.OrganizationInvite;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, String> {
  Page<OrganizationInvite> findAllByInvitedUserAndStatusOrderByCreationDatetimeDesc(
      String invitedUser, OrganizationInviteStatus inviteStatus, Pageable pageable);

  @Query("update OrganizationInvite oI set oI.status = ?2 where oI.id = ?1")
  @Modifying
  void updateOrganizationInvite(String id, OrganizationInviteStatus status);

  @Query(
      "select oInvite from OrganizationInvite  oInvite where oInvite.inviterOrg = ?1 and"
          + " oInvite.invitedUser = ?2 and oInvite.status = ?3 order by oInvite.creationDatetime"
          + " desc limit 1")
  Optional<OrganizationInvite> findLatestInviteByCriteria(
      String inviterOrg, String invitedUser, OrganizationInviteStatus status);

  void deleteAllByInviterOrgAndInvitedUser(String inviterOrg, String invitedUser);

  void deleteOrganizationInviteById(String id);

  boolean existsByInviterOrgAndInvitedUserAndStatus(
      String inviterOrg, String invitedUser, OrganizationInviteStatus status);
}
