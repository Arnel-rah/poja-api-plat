package api.poja.io.endpoint.rest.controller;

import static api.poja.io.model.RangedInstant.getRangedInstant;

import api.poja.io.endpoint.rest.mapper.BillingInfoMapper;
import api.poja.io.endpoint.rest.mapper.OrganizationInviteMapper;
import api.poja.io.endpoint.rest.mapper.OrganizationMapper;
import api.poja.io.endpoint.rest.mapper.UserMapper;
import api.poja.io.endpoint.rest.mapper.UserPaymentSetupStateMapper;
import api.poja.io.endpoint.rest.mapper.UserStateMapper;
import api.poja.io.endpoint.rest.model.CreateUsersRequestBody;
import api.poja.io.endpoint.rest.model.CreateUsersResponse;
import api.poja.io.endpoint.rest.model.GetUserPaymentSetupStateResponse;
import api.poja.io.endpoint.rest.model.GetUserResponse;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.OrgBillingInfo;
import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.endpoint.rest.model.OrganizationInvite;
import api.poja.io.endpoint.rest.model.PagedOrganizationInvites;
import api.poja.io.endpoint.rest.model.PagedOrganizations;
import api.poja.io.endpoint.rest.model.PagedOrgsBillingInfoResponse;
import api.poja.io.endpoint.rest.model.PagedUsers;
import api.poja.io.endpoint.rest.model.SortOrder;
import api.poja.io.endpoint.rest.model.SortUsersBy;
import api.poja.io.endpoint.rest.model.UpdateOrganizationInviteRequestBody;
import api.poja.io.endpoint.rest.model.UserBillingInfo;
import api.poja.io.endpoint.rest.model.UserStatesResponse;
import api.poja.io.endpoint.rest.model.UserStatistics;
import api.poja.io.endpoint.validator.OrganizationValidator;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.RangedInstant;
import api.poja.io.model.page.Page;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.UserPaymentSetupStateService;
import api.poja.io.service.UserService;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.service.organization.OrganizationUsersService;
import api.poja.io.service.workflows.userState.UserStateService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserService service;
  private final OrganizationService organizationService;
  private final OrganizationUsersService orgUsersService;
  private final UserMapper mapper;
  private final OrganizationMapper organizationMapper;
  private final OrganizationInviteMapper organizationInviteMapper;
  private final BillingInfoService billingInfoService;
  private final BillingInfoMapper billingInfoMapper;
  private final OrganizationValidator organizationValidator;
  private final UserStateService userStateService;
  private final UserStateMapper userStateMapper;
  private final UserPaymentSetupStateMapper userPaymentSetupStateMapper;
  private final UserPaymentSetupStateService userPaymentSetupStateService;

  @PostMapping("/users")
  public CreateUsersResponse signUp(@RequestBody CreateUsersRequestBody toCreate) {
    var data =
        service.createUsers(Objects.requireNonNull(toCreate.getData())).stream()
            .map(mapper::toRest)
            .toList();
    return new CreateUsersResponse().data(data);
  }

  @GetMapping("/users/{userId}/billing")
  public UserBillingInfo getUserBillingInfo(
      @PathVariable String userId,
      @RequestParam(required = false) Instant startTime,
      @RequestParam(required = false) Instant endTime,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) MonthType month) {
    RangedInstant datetimeRange = getRangedInstant(startTime, endTime, year, month);
    return billingInfoMapper.toUserRest(
        billingInfoService.getUserBillingInfo(userId, datetimeRange),
        datetimeRange.start(),
        datetimeRange.end());
  }

  @GetMapping("/users/{userId}/orgs")
  public PagedOrganizations getUserOrgs(
      @PathVariable String userId,
      @RequestParam(required = false, name = "page", defaultValue = "1") PageFromOne pageFromOne,
      @RequestParam(required = false, name = "page_size", defaultValue = "10")
          BoundedPageSize boundedPageSize) {
    var pagedResults = orgUsersService.getUserOrgs(pageFromOne, boundedPageSize, userId);
    var data = pagedResults.data().stream().map(organizationMapper::toRest).toList();
    return new PagedOrganizations()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @GetMapping("/users/{userId}/orgs/billings")
  public PagedOrgsBillingInfoResponse getUserOrgsBilling(
      @PathVariable String userId,
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "10", name = "page_size")
          BoundedPageSize pageSize,
      @RequestParam(required = false) Instant startTime,
      @RequestParam(required = false) Instant endTime,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) MonthType month) {
    RangedInstant datetimeRange = getRangedInstant(startTime, endTime, year, month);
    Page<OrgBillingInfo> data =
        billingInfoService
            .getOrgsBillingInfo(userId, page, pageSize, datetimeRange)
            .map(b -> billingInfoMapper.toOrgRest(b, datetimeRange.start(), datetimeRange.end()));
    return new PagedOrgsBillingInfoResponse()
        .pageNumber(data.queryPage().getValue())
        .pageSize(data.queryPageSize().getValue())
        .hasPrevious(data.hasPrevious())
        .count(data.count())
        .data(data.data().stream().toList());
  }

  @PutMapping("/users/{userId}/orgs")
  public List<Organization> crupdateUserOrgs(
      @PathVariable String userId, @RequestBody List<Organization> orgs) {
    orgs.forEach(organizationValidator);
    return organizationService.crupdateOrgs(userId, orgs).stream()
        .map(organizationMapper::toRest)
        .toList();
  }

  @GetMapping("/users")
  public PagedUsers getUsers(
      @RequestParam(value = "username", required = false, defaultValue = "") String username,
      @RequestParam(required = false) SortUsersBy sortBy,
      @RequestParam(required = false) SortOrder sortOrder,
      @RequestParam(value = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10")
          BoundedPageSize pageSize) {
    var pagedResults = service.getUsers(username, sortBy, sortOrder, page, pageSize);
    var data = pagedResults.data().stream().map(mapper::toGetUserResponse).toList();
    return new PagedUsers()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @GetMapping("/users/{userId}")
  public GetUserResponse getUserById(@PathVariable String userId) {
    return mapper.toGetUserResponse(service.getUserById(userId));
  }

  @GetMapping("/users/{userId}/statuses")
  public UserStatesResponse getUserStates(@PathVariable String userId) {
    var data = userStateService.findAllByUserId(userId);
    return new UserStatesResponse().data(data.stream().map(userStateMapper::toRest).toList());
  }

  @GetMapping("/users/{userId}/org-invites")
  public PagedOrganizationInvites getUserOrganizationInvites(
      @PathVariable String userId,
      @RequestParam(value = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", required = false, defaultValue = "10")
          BoundedPageSize pageSize) {
    var pagedResults = orgUsersService.getUserOrgInvites(userId, page, pageSize);
    var data = pagedResults.data().stream().map(organizationInviteMapper::toRest).toList();
    return new PagedOrganizationInvites()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @PutMapping("/users/{userId}/org-invites")
  public OrganizationInvite updateUserOrganizationInvites(
      @PathVariable String userId, @RequestBody UpdateOrganizationInviteRequestBody toUpdate) {
    return organizationInviteMapper.toRest(orgUsersService.updateUserOrgInvites(userId, toUpdate));
  }

  @GetMapping("/user-stats")
  public UserStatistics getUserStatistics() {
    return service.getUserStats();
  }

  @GetMapping("/users/{userId}/payment-setup-states")
  public GetUserPaymentSetupStateResponse getUserPaymentSetupStates(@PathVariable String userId) {
    var data =
        userPaymentSetupStateService.getSortedStatesByUserId(userId).stream()
            .map(userPaymentSetupStateMapper::toRest)
            .toList();
    return new GetUserPaymentSetupStateResponse().data(data);
  }
}
