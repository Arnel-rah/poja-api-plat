package api.poja.io.service.organization;

import static api.poja.io.repository.model.enums.OrganizationInviteStatus.ACCEPTED;

import api.poja.io.aws.iam.model.ConsoleUserCredentials;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.OrganizationUpserted;
import api.poja.io.endpoint.rest.mapper.OrganizationMapper;
import api.poja.io.endpoint.rest.model.OrganizationInviteType;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.OrganizationDTO;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.OrganizationInviteRepository;
import api.poja.io.repository.jpa.OrganizationRepository;
import api.poja.io.repository.model.Organization;
import api.poja.io.repository.model.OrganizationInvite;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import api.poja.io.service.validator.UserOrgThresholdValidator;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrganizationService {
  private final OrganizationRepository repository;
  private final OrganizationInviteRepository organizationInviteRepository;
  private final OrganizationMapper mapper;
  private final UserOrgThresholdValidator thresholdValidator;
  private final EventProducer<OrganizationUpserted> organizationUpsertedProducer;

  public OrganizationDTO getOrganizationByIdWithMembersCount(String orgId) {
    return repository
        .findByIdWithMembersCount(orgId)
        .orElseThrow(
            () -> new NotFoundException("Organization with id " + orgId + " is not found"));
  }

  public List<OrganizationDTO> crupdateOrgs(
      String userId, List<api.poja.io.endpoint.rest.model.Organization> orgs) {
    thresholdValidator.accept(
        userId, orgs.stream().map(api.poja.io.endpoint.rest.model.Organization::getId).toList());
    var toSave = orgs.stream().map(org -> mapper.toDomain(org, userId)).toList();
    var toUpsert = toSave.stream().filter(o -> !repository.existsById(o.getId())).toList();
    var savedOrgs = repository.saveAll(toSave);
    savedOrgs.forEach(this::createInviteIfNewOrganization);
    var events = toUpsert.stream().map(OrganizationUpserted::new).toList();
    organizationUpsertedProducer.accept(events);
    return savedOrgs.stream()
        .map(
            org ->
                new OrganizationDTO(
                    org.getId(),
                    org.getName(),
                    org.getCreationDatetime(),
                    org.getOwnerId(),
                    repository.countAcceptedAndPendingMembersCountByOrgId(org.getId())))
        .toList();
  }

  private void createInviteIfNewOrganization(Organization organization) {
    Optional<OrganizationInvite> orgOwnerInvite =
        organizationInviteRepository.findLatestInviteByCriteria(
            organization.getId(), organization.getOwnerId(), ACCEPTED);
    if (orgOwnerInvite.isEmpty()) {
      var newInvite =
          OrganizationInvite.builder()
              .invitedUser(organization.getOwnerId())
              .inviterOrg(organization.getId())
              .status(ACCEPTED)
              .build();

      organizationInviteRepository.save(newInvite);
    }
  }

  public Optional<Organization> findById(String orgId) {
    return repository.findById(orgId);
  }

  public Organization getById(String orgId) {
    return repository
        .findById(orgId)
        .orElseThrow(
            () -> new NotFoundException("Organization with identifier " + orgId + " is not found"));
  }

  public List<Organization> findAllByOwnerId(String userId) {
    return repository.findAllByOwnerId(userId);
  }

  @Transactional
  public void updateConsoleInformations(String orgId, String groupName, String policyDocumentName) {
    repository.updateConsoleInformations(orgId, groupName, policyDocumentName);
  }

  public Page<OrganizationInvite> getPaginatedOrganizationInvitesByStatus(
      String orgId,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize,
      OrganizationInviteType status) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());
    var data =
        repository
            .findLatestInvitesByOrgIdAndStatus(
                orgId, OrganizationInviteStatus.valueOf(status.getValue()), pageable)
            .getContent();
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  @Transactional
  public OrganizationInvite cancelOrganizationInvite(String inviteId) {
    var invite =
        organizationInviteRepository
            .findById(inviteId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Organization invite with id " + inviteId + " is not found"));

    organizationInviteRepository.deleteOrganizationInviteById(invite.getId());

    return invite;
  }

  @Transactional
  public void updateConsoleCredentials(
      String orgId, ConsoleUserCredentials credentials, String policyDocumentName) {
    repository.updateConsoleCredentials(
        orgId,
        credentials.accountId(),
        credentials.username(),
        credentials.password(),
        policyDocumentName);
  }

  public List<Organization> getAllByOwnerId(String ownerId) {
    return repository.getAllByOwnerId(ownerId);
  }
}
