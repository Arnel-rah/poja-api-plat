package api.poja.io.endpoint.rest.controller;

import static api.poja.io.model.RangedInstant.getRangedInstant;

import api.poja.io.endpoint.rest.mapper.BillingInfoMapper;
import api.poja.io.endpoint.rest.mapper.OrganizationInviteMapper;
import api.poja.io.endpoint.rest.mapper.OrganizationMapper;
import api.poja.io.endpoint.rest.mapper.OrganizationSetupStateMapper;
import api.poja.io.endpoint.rest.mapper.UserMapper;
import api.poja.io.endpoint.rest.model.CrupdateOrganizationMembersRequestBody;
import api.poja.io.endpoint.rest.model.GetOrganizationSetupStates;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.OrgBillingInfo;
import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.endpoint.rest.model.OrganizationInvite;
import api.poja.io.endpoint.rest.model.OrganizationInviteType;
import api.poja.io.endpoint.rest.model.PagedOrganizationInvites;
import api.poja.io.endpoint.rest.model.PagedUsers;
import api.poja.io.endpoint.rest.model.User;
import api.poja.io.endpoint.rest.security.model.Principal;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.RangedInstant;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.OrganizationSetupStateService;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.service.organization.OrganizationUsersService;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class OrganizationController {
  private final OrganizationService service;
  private final OrganizationUsersService orgUsersService;
  private final UserService userService;
  private final OrganizationMapper mapper;
  private final OrganizationInviteMapper organizationInviteMapper;
  private final UserMapper userMapper;
  private final BillingInfoService billingInfoService;
  private final BillingInfoMapper billingInfoMapper;
  private final OrganizationSetupStateService organizationSetupStateService;
  private final OrganizationSetupStateMapper organizationSetupStateMapper;

  @GetMapping("/orgs/{orgId}/users")
  public PagedUsers getOrgMembers(
      @PathVariable String orgId,
      @RequestParam(value = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10")
          BoundedPageSize pageSize) {
    var pagedResults = orgUsersService.getPaginatedOrgMembers(page, pageSize, orgId);
    var data = pagedResults.data().stream().map(userMapper::toGetUserResponse).toList();
    return new PagedUsers()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @GetMapping("/orgs/{orgId}")
  public Organization getOrganizationById(@PathVariable String orgId) {
    return mapper.toRest(service.getOrganizationByIdWithMembersCount(orgId));
  }

  @GetMapping("/orgs/{orgId}/billing")
  public OrgBillingInfo getUserBillingInfo(
      @PathVariable String orgId,
      @RequestParam(required = false) Instant startTime,
      @RequestParam(required = false) Instant endTime,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) MonthType month) {
    RangedInstant datetimeRange = getRangedInstant(startTime, endTime, year, month);
    return billingInfoMapper.toOrgRest(
        billingInfoService.getOrgBillingInfo(orgId, datetimeRange),
        datetimeRange.start(),
        datetimeRange.end());
  }

  @PutMapping("/orgs/{orgId}/users")
  public List<User> crupdateOrgMembers(
      @PathVariable String orgId,
      @RequestBody List<CrupdateOrganizationMembersRequestBody> toCrupdate,
      @AuthenticationPrincipal Principal principal) {
    return orgUsersService.crupdateOrgMembers(principal.getUser(), orgId, toCrupdate).stream()
        .map(userMapper::toRest)
        .toList();
  }

  @GetMapping("/orgs/{orgId}/invitees/suggestions")
  public PagedUsers getOrganizationInviteesSuggestions(
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(value = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10")
          BoundedPageSize pageSize,
      @PathVariable String orgId) {
    var pagedResults = userService.getUsers(orgId, username, page, pageSize);
    var data = pagedResults.data().stream().map(userMapper::toGetUserResponse).toList();
    return new PagedUsers()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @GetMapping("/orgs/{orgId}/invites")
  public PagedOrganizationInvites getOrganizationPendingInvites(
      @RequestParam(value = "status", required = false, defaultValue = "PENDING")
          OrganizationInviteType status,
      @RequestParam(value = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10")
          BoundedPageSize pageSize,
      @PathVariable String orgId) {
    var pagedResults =
        service.getPaginatedOrganizationInvitesByStatus(orgId, page, pageSize, status);
    var data = pagedResults.data().stream().map(organizationInviteMapper::toRest).toList();
    return new PagedOrganizationInvites()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @DeleteMapping("/orgs/{orgId}/invites/{inviteId}")
  public OrganizationInvite cancelOrganizationInvite(
      @PathVariable String orgId, @PathVariable String inviteId) {
    return organizationInviteMapper.toRest(service.cancelOrganizationInvite(inviteId));
  }

  @GetMapping("/orgs/{orgId}/setup-states")
  public GetOrganizationSetupStates getOrganizationSetupStates(@PathVariable String orgId) {
    var data =
        organizationSetupStateService.getSortedStatesByOrgId(orgId).stream()
            .map(organizationSetupStateMapper::toRest)
            .toList();

    return new GetOrganizationSetupStates().data(data);
  }
}
