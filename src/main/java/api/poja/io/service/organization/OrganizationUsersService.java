package api.poja.io.service.organization;

import static api.poja.io.repository.model.enums.OrganizationInviteStatus.ACCEPTED;
import static api.poja.io.repository.model.enums.OrganizationInviteStatus.PENDING;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.ConsoleUserCredentialsUpdateRequested;
import api.poja.io.endpoint.event.model.OrgInvitationEmailNotificationRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.rest.model.CrupdateOrganizationMembersRequestBody;
import api.poja.io.endpoint.rest.model.UpdateOrganizationInviteRequestBody;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.OrganizationDTO;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.User;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.UserRepository;
import api.poja.io.repository.jpa.OrganizationInviteRepository;
import api.poja.io.repository.jpa.OrganizationRepository;
import api.poja.io.repository.model.OrganizationInvite;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import api.poja.io.service.UserService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrganizationUsersService {
  private static final String SUBJECT = "POJA invitation organization";

  private final EventProducer<PojaEvent> eventProducer;

  private final OrganizationService organizationService;
  private final UserService userService;
  private final UserRepository userRepository;
  private final OrganizationRepository repository;
  private final OrganizationInviteRepository organizationInviteRepository;

  public OrganizationUsersService(
      OrganizationService organizationService,
      UserService userService,
      UserRepository userRepository,
      OrganizationRepository repository,
      OrganizationInviteRepository organizationInviteRepository,
      EventProducer<PojaEvent> eventProducer) {
    this.organizationService = organizationService;
    this.userService = userService;
    this.userRepository = userRepository;
    this.repository = repository;
    this.organizationInviteRepository = organizationInviteRepository;
    this.eventProducer = eventProducer;
  }

  public Page<User> getPaginatedOrgMembers(
      PageFromOne pageFromOne, BoundedPageSize boundedPageSize, String orgId) {
    return userRepository.getPaginatedUsersByOrgIdAndInviteStatus(
        orgId, ACCEPTED, pageFromOne, boundedPageSize);
  }

  public List<User> getOrgMembers(String orgId) {
    return userRepository.getAllUsersByOrgIdAndInviteStatus(orgId, ACCEPTED);
  }

  public Page<OrganizationDTO> getUserOrgs(
      PageFromOne pageFromOne, BoundedPageSize boundedPageSize, String userId) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());
    var data =
        repository.findAllOrganizationsByUserIdAndStatus(userId, ACCEPTED, pageable).toList();
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  public Page<OrganizationInvite> getUserOrgInvites(
      String userId, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());
    var data =
        organizationInviteRepository
            .findAllByInvitedUserAndStatusOrderByCreationDatetimeDesc(userId, PENDING, pageable)
            .toList();
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  @Transactional
  public OrganizationInvite updateUserOrgInvites(
      String userId, UpdateOrganizationInviteRequestBody toUpdate) {
    assert userId != null : "userId is null";
    var newStatus =
        OrganizationInviteStatus.valueOf(Objects.requireNonNull(toUpdate.getType()).getValue());

    organizationInviteRepository.updateOrganizationInvite(toUpdate.getId(), newStatus);

    return findInviteById(toUpdate.getId());
  }

  @Transactional
  public List<User> crupdateOrgMembers(
      User authenticatedUser,
      String orgId,
      List<CrupdateOrganizationMembersRequestBody> toCrupdate) {
    return toCrupdate.stream()
        .map(movement -> crupdateOrgMember(authenticatedUser, orgId, movement))
        .toList();
  }

  private User crupdateOrgMember(
      User authenticatedUser, String orgId, CrupdateOrganizationMembersRequestBody toCrupdate) {
    var idUser = Objects.requireNonNull(toCrupdate.getIdUser());
    var movementType = Objects.requireNonNull(toCrupdate.getMovementType());
    return switch (movementType) {
      case ADD -> addUserToOrg(orgId, idUser);
      case REMOVE -> removeUserFromOrg(authenticatedUser, orgId, idUser);
    };
  }

  // TODO, if done in batch, validate each item with exist by before saveAll
  private User addUserToOrg(String orgId, String idUser) {
    var org = organizationService.getById(orgId);
    User user = userService.getUserById(idUser);

    if (idUser.equals(org.getOwnerId())) {
      return user;
    }

    if (organizationInviteRepository.existsByInviterOrgAndInvitedUserAndStatus(
        orgId, idUser, PENDING)) {
      throw new BadRequestException(
          "user.username = "
              + user.getUsername()
              + " has already been invited and has not reacted to invite yet");
    }
    var invite =
        OrganizationInvite.builder().inviterOrg(orgId).invitedUser(idUser).status(PENDING).build();
    organizationInviteRepository.save(invite);

    sendOrgInvitationEmailNotif(invite);
    return user;
  }

  @SneakyThrows
  private void sendOrgInvitationEmailNotif(OrganizationInvite invite) {

    var invitedUserId =
        userRepository
            .findById(invite.getInvitedUser(), now())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Invited user with identifier "
                            + invite.getInvitedUser()
                            + " is not found"));

    var to = invitedUserId.getEmail();
    eventProducer.accept(
        List.of(
            new OrgInvitationEmailNotificationRequested(
                to, SUBJECT, invite.getInviterOrg(), invite.getInvitedUser())));
  }

  public Optional<OrganizationInvite> findLatestUserInvite(
      String orgId, String userId, OrganizationInviteStatus status) {
    return organizationInviteRepository.findLatestInviteByCriteria(orgId, userId, status);
  }

  public boolean isOrgMember(String orgId, String userId) {
    Optional<OrganizationInvite> orgInvite =
        organizationInviteRepository.findLatestInviteByCriteria(orgId, userId, ACCEPTED);

    return orgInvite.isPresent();
  }

  private User removeUserFromOrg(User authenticatedUser, String orgId, String idUser) {
    var org = organizationService.getById(orgId);
    if (!Objects.equals(authenticatedUser.getId(), org.getOwnerId())
        && !Objects.equals(authenticatedUser.getId(), idUser)) {
      throw new BadRequestException("basic users cannot remove users other than themselves");
    }
    if (idUser.equals(org.getOwnerId())) {
      throw new BadRequestException("Organization owner cannot be removed");
    }
    organizationInviteRepository.deleteAllByInviterOrgAndInvitedUser(orgId, idUser);
    eventProducer.accept(List.of(new ConsoleUserCredentialsUpdateRequested(orgId)));
    return userService.getUserById(idUser);
  }

  public OrganizationInvite findInviteById(String inviteId) {
    return organizationInviteRepository
        .findById(inviteId)
        .orElseThrow(
            () -> new NotFoundException("Invite with identifier " + inviteId + " is not found"));
  }
}
