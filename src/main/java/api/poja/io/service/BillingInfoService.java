package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.SortBy.COMPUTED_BILLING_PRICE;
import static api.poja.io.endpoint.rest.model.SortBy.COST;
import static api.poja.io.endpoint.rest.model.SortBy.COST_MARGIN;
import static api.poja.io.endpoint.rest.model.SortBy.JOIN_DATE;
import static api.poja.io.endpoint.rest.model.SortBy.LAST_CONNECTION;
import static api.poja.io.endpoint.rest.model.SortOrder.DESC;
import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNullElse;

import api.poja.io.endpoint.rest.model.SortBy;
import api.poja.io.endpoint.rest.model.SortOrder;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.RangedInstant;
import api.poja.io.model.User;
import api.poja.io.model.billing.AggregatedBillingInfoByEnvDTOProjection;
import api.poja.io.model.billing.AggregatedBillingInfoByOrgDTOProjection;
import api.poja.io.model.billing.AggregatedBillingInfoByUserDTOProjection;
import api.poja.io.model.billing.AggregatedBillingInfoWithAwsByUserDTOProjection;
import api.poja.io.model.billing.AggregatedOrgBillingInfoByEnvDTOProjection;
import api.poja.io.model.billing.UserBillingInfoWithAws;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.BillingInfoRepository;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.repository.model.Organization;
import api.poja.io.repository.model.enums.BillingInfoComputeStatus;
import api.poja.io.service.organization.OrganizationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class BillingInfoService {
  private final BillingInfoRepository repository;
  private final UserService userService;
  private final OrganizationService organizationService;

  public BillingInfo getOrgBillingInfoByEnvironment(
      String orgId, String appId, String envId, RangedInstant datetimeRange) {
    var org = organizationService.getById(orgId);
    return getOrgBillingInfoByEnvironment(org, appId, envId, datetimeRange);
  }

  public BillingInfo getOrgBillingInfoByEnvironment(
      Organization org, String appId, String envId, RangedInstant datetimeRange) {
    var user = userService.getUserById(org.getOwnerId());
    var optionalSumByEnvWithMaxDatetime =
        repository.computeSumByEnv(
            org.getId(), appId, envId, datetimeRange.start(), datetimeRange.end());
    if (optionalSumByEnvWithMaxDatetime.isEmpty()) {
      return BillingInfo.builder()
          .orgId(org.getId())
          .appId(appId)
          .envId(envId)
          .pricingMethod(user.getPricingMethod())
          .computedPrice(ZERO)
          .computedDurationInMinutes(0.0)
          .computeDatetime(null)
          .computationIntervalEnd(null)
          .build();
    }
    var sumByEnvWithMaxDatetime = optionalSumByEnvWithMaxDatetime.get();
    Instant maxComputeDatetime = sumByEnvWithMaxDatetime.getMaxComputeDatetime();
    return BillingInfo.builder()
        .orgId(org.getId())
        .appId(appId)
        .envId(envId)
        .pricingMethod(user.getPricingMethod())
        .computedPrice(requireNonNullElse(sumByEnvWithMaxDatetime.getAmount(), ZERO))
        .computedDurationInMinutes(
            requireNonNullElse(sumByEnvWithMaxDatetime.getComputedDurationInMinutes(), 0.0))
        .computeDatetime(maxComputeDatetime)
        .computationIntervalEnd(maxComputeDatetime)
        .build();
  }

  public List<BillingInfo> getUserBillingInfoByApplication(
      String userId, String appId, RangedInstant datetimeRange) {
    var user = userService.getUserById(userId);
    return getUserBillingInfoByApplication(user, appId, datetimeRange);
  }

  public List<BillingInfo> getUserBillingInfoByApplication(
      User user, String appId, RangedInstant datetimeRange) {
    String userId = user.getId();
    List<AggregatedBillingInfoByEnvDTOProjection> sumsByEnvs =
        repository.computeSumForUserByAppGroupedByEnv(
            userId, appId, datetimeRange.start(), datetimeRange.end());

    return sumsByEnvs.stream()
        .map(
            sumByEnvWithMaxDatetime -> {
              Instant computeDatetime = sumByEnvWithMaxDatetime.getMaxComputeDatetime();
              return BillingInfo.builder()
                  .pricingMethod(user.getPricingMethod())
                  .userId(userId)
                  .appId(appId)
                  .envId(sumByEnvWithMaxDatetime.getEnvId())
                  .computedPrice(requireNonNullElse(sumByEnvWithMaxDatetime.getAmount(), ZERO))
                  .computedMemoryDurationInMbMinutes(
                      requireNonNullElse(sumByEnvWithMaxDatetime.getMemoryDuration(), ZERO))
                  .computedDurationInMinutes(
                      requireNonNullElse(
                          sumByEnvWithMaxDatetime.getComputedDurationInMinutes(), 0.0))
                  .computeDatetime(computeDatetime)
                  .computationIntervalEnd(computeDatetime)
                  .build();
            })
        .toList();
  }

  public List<BillingInfo> getOrgBillingInfoByApplication(
      String orgId, String appId, RangedInstant datetimeRange) {
    var org = organizationService.getById(orgId);
    return getOrgBillingInfoByApplication(org, appId, datetimeRange);
  }

  public List<BillingInfo> getOrgBillingInfoByApplication(
      Organization org, String appId, RangedInstant datetimeRange) {
    String orgId = org.getId();
    User user = userService.getUserById(org.getOwnerId());
    List<AggregatedOrgBillingInfoByEnvDTOProjection> sumsByEnv =
        repository.computeSumForOrgByAppGroupedByEnv(
            orgId, appId, datetimeRange.start(), datetimeRange.end());

    return sumsByEnv.stream()
        .map(
            sumByEnvWithMaxDatetime -> {
              Instant computeDatetime = sumByEnvWithMaxDatetime.getMaxComputeDatetime();
              return BillingInfo.builder()
                  .pricingMethod(user.getPricingMethod())
                  .orgId(orgId)
                  .appId(appId)
                  .envId(sumByEnvWithMaxDatetime.getEnvId())
                  .computedPrice(requireNonNullElse(sumByEnvWithMaxDatetime.getAmount(), ZERO))
                  .computedDurationInMinutes(
                      requireNonNullElse(
                          sumByEnvWithMaxDatetime.getComputedDurationInMinutes(), 0.0))
                  .computeDatetime(computeDatetime)
                  .computationIntervalEnd(computeDatetime)
                  .build();
            })
        .toList();
  }

  public BillingInfo getUserBillingInfo(String userId, RangedInstant datetimeRange) {
    User user = userService.getUserById(userId);
    var pricingMethod = user.getPricingMethod();
    var optionalUserBillingInfoAggregated =
        repository.computeSumByUserGroupedByUser(
            userId, datetimeRange.start(), datetimeRange.end());
    if (optionalUserBillingInfoAggregated.isEmpty()) {
      return BillingInfo.builder()
          .userId(user.getId())
          .pricingMethod(user.getPricingMethod())
          .computedPrice(ZERO)
          .computedDurationInMinutes(0.0)
          .computeDatetime(null)
          .computationIntervalEnd(null)
          .build();
    }
    AggregatedBillingInfoByUserDTOProjection aggregatedBillingInfoByUserDTOProjection =
        optionalUserBillingInfoAggregated.get();
    Instant computeDatetime = aggregatedBillingInfoByUserDTOProjection.getMaxComputeDatetime();
    return BillingInfo.builder()
        .computedPrice(
            requireNonNullElse(aggregatedBillingInfoByUserDTOProjection.getAmount(), ZERO))
        .computedDurationInMinutes(
            requireNonNullElse(
                aggregatedBillingInfoByUserDTOProjection.getComputedDurationInMinutes(), 0.0))
        .pricingMethod(pricingMethod)
        .pricingMethod(user.getPricingMethod())
        .userId(userId)
        .computeDatetime(computeDatetime)
        .computationIntervalEnd(computeDatetime)
        .build();
  }

  public BillingInfo getOrgBillingInfo(String orgId, RangedInstant datetimeRange) {
    var org = organizationService.getById(orgId);
    var userId = org.getOwnerId();
    User user = userService.getUserById(userId);
    var pricingMethod = user.getPricingMethod();
    var optionalOrgBillingInfoAggregated =
        repository.computeSumByOrgGroupedByOrg(orgId, datetimeRange.start(), datetimeRange.end());
    if (optionalOrgBillingInfoAggregated.isEmpty()) {
      return BillingInfo.builder()
          .userId(user.getId())
          .orgId(orgId)
          .pricingMethod(user.getPricingMethod())
          .computedPrice(ZERO)
          .computedDurationInMinutes(0.0)
          .computeDatetime(null)
          .computationIntervalEnd(null)
          .build();
    }
    AggregatedBillingInfoByOrgDTOProjection aggregatedBillingInfoByOrgDTOProjection =
        optionalOrgBillingInfoAggregated.get();
    Instant computeDatetime = aggregatedBillingInfoByOrgDTOProjection.getMaxComputeDatetime();
    return BillingInfo.builder()
        .orgId(orgId)
        .computedPrice(
            requireNonNullElse(aggregatedBillingInfoByOrgDTOProjection.getAmount(), ZERO))
        .computedDurationInMinutes(
            requireNonNullElse(
                aggregatedBillingInfoByOrgDTOProjection.getComputedDurationInMinutes(), 0.0))
        .pricingMethod(pricingMethod)
        .pricingMethod(user.getPricingMethod())
        .userId(userId)
        .computeDatetime(computeDatetime)
        .computationIntervalEnd(computeDatetime)
        .build();
  }

  public Page<UserBillingInfoWithAws> getUsersBillingInfo(
      PageFromOne pageFromOne,
      BoundedPageSize pageSize,
      RangedInstant datetimeRange,
      Boolean archived,
      String username,
      SortBy sortBy,
      SortOrder sortOrder,
      RangedInstant joinedRange) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, pageSize.getValue());
    String order = sortOrder == null ? DESC.getValue() : sortOrder.getValue();
    Instant now = now();
    List<AggregatedBillingInfoWithAwsByUserDTOProjection> sumsByUser =
        computeSumByUsers(
            datetimeRange, archived, username, sortBy, pageable, order, joinedRange, now);
    return new Page<>(
        pageFromOne,
        pageSize,
        sumsByUser.stream()
            .map(
                (sumByUserWithMaxDatetime) -> {
                  Instant computeDatetime = sumByUserWithMaxDatetime.getMaxComputeDatetime();
                  return UserBillingInfoWithAws.builder()
                      .pricingMethod(sumByUserWithMaxDatetime.getPricingMethod())
                      .userId(sumByUserWithMaxDatetime.getUserId())
                      // .appId(null)
                      // .envId(null)
                      .computedPrice(requireNonNullElse(sumByUserWithMaxDatetime.getAmount(), ZERO))
                      .computedDurationInMinutes(
                          requireNonNullElse(
                              sumByUserWithMaxDatetime.getComputedDurationInMinutes(), 0.0))
                      .computeDatetime(computeDatetime)
                      .computationIntervalEnd(computeDatetime)
                      .awsCost(sumByUserWithMaxDatetime.getAwsCost())
                      .awsCostUpdateDatetime(sumByUserWithMaxDatetime.getAwsCostUpdateDatetime())
                      .costMargin(sumByUserWithMaxDatetime.getCostMargin())
                      .build();
                })
            .toList());
  }

  private List<AggregatedBillingInfoWithAwsByUserDTOProjection> computeSumByUsers(
      RangedInstant datetimeRange,
      Boolean archived,
      String username,
      SortBy sortBy,
      Pageable pageable,
      String order,
      RangedInstant joinedRange,
      Instant now) {
    var yearMonth = YearMonth.from(datetimeRange.start().atZone(UTC));
    var year = yearMonth.getYear();
    var month = yearMonth.getMonth().name();
    if (sortBy == null || COMPUTED_BILLING_PRICE.equals(sortBy)) {
      return repository.computeSumByUserGroupedByUserAndSortedByBilling(
          datetimeRange.start(),
          datetimeRange.end(),
          pageable.getPageSize(),
          pageable.getOffset(),
          archived,
          username,
          order,
          joinedRange.start(),
          joinedRange.end(),
          year,
          month);
    }
    if (JOIN_DATE.equals(sortBy)) {
      return repository.computeSumByUserGroupedByUserAndSortedByJoinDate(
          datetimeRange.start(),
          datetimeRange.end(),
          pageable.getPageSize(),
          pageable.getOffset(),
          archived,
          username,
          order,
          joinedRange.start(),
          joinedRange.end(),
          year,
          month);
    }
    if (LAST_CONNECTION.equals(sortBy)) {
      return repository.computeSumByUserGroupedByUserAndSortedByLastConnection(
          datetimeRange.start(),
          datetimeRange.end(),
          pageable.getPageSize(),
          pageable.getOffset(),
          archived,
          username,
          order,
          joinedRange.start(),
          joinedRange.end(),
          year,
          month);
    }
    if (COST.equals(sortBy)) {
      return repository.computeSumByUserGroupedByUserAndSortedByUserCost(
          datetimeRange.start(),
          datetimeRange.end(),
          pageable.getPageSize(),
          pageable.getOffset(),
          archived,
          username,
          order,
          joinedRange.start(),
          joinedRange.end(),
          year,
          month);
    }
    if (COST_MARGIN.equals(sortBy)) {
      return repository.computeSumByUserGroupedByUserAndSortedByUserCostMargin(
          datetimeRange.start(),
          datetimeRange.end(),
          pageable.getPageSize(),
          pageable.getOffset(),
          archived,
          username,
          order,
          joinedRange.start(),
          joinedRange.end(),
          year,
          month);
    }
    return repository.computeSumByUserGroupedByUserAndSortedBySuspension(
        datetimeRange.start(),
        datetimeRange.end(),
        pageable.getPageSize(),
        pageable.getOffset(),
        archived,
        username,
        order,
        joinedRange.start(),
        joinedRange.end(),
        now,
        year,
        month);
  }

  public Page<BillingInfo> getOrgsBillingInfo(
      String userId,
      PageFromOne pageFromOne,
      BoundedPageSize pageSize,
      RangedInstant datetimeRange) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, pageSize.getValue());
    List<AggregatedBillingInfoByOrgDTOProjection> sumsByOrg =
        repository.computeSumByOrgGroupedByOrgAndUser(
            userId,
            datetimeRange.start(),
            datetimeRange.end(),
            pageable.getPageSize(),
            pageable.getOffset());
    return new Page<>(
        pageFromOne,
        pageSize,
        sumsByOrg.stream()
            .map(
                sumByOrgWithMaxDatetime -> {
                  Instant computeDatetime = sumByOrgWithMaxDatetime.getMaxComputeDatetime();
                  return BillingInfo.builder()
                      .pricingMethod(sumByOrgWithMaxDatetime.getPricingMethod())
                      .userId(sumByOrgWithMaxDatetime.getUserId())
                      .orgId(sumByOrgWithMaxDatetime.getOrgId())
                      // .appId(null)
                      // .envId(null)
                      .computedPrice(requireNonNullElse(sumByOrgWithMaxDatetime.getAmount(), ZERO))
                      .computedDurationInMinutes(
                          requireNonNullElse(
                              sumByOrgWithMaxDatetime.getComputedDurationInMinutes(), 0.0))
                      .computeDatetime(computeDatetime)
                      .computationIntervalEnd(computeDatetime)
                      .build();
                })
            .toList());
  }

  public BillingInfo crupdateBillingInfo(BillingInfo toSave) {
    return repository.save(toSave);
  }

  @Transactional
  public void updateBillingInfoAfterCalculation(
      BillingInfoComputeStatus status,
      Instant computeDatetime,
      Double computedDurationInMinutes,
      BigDecimal computedPrice,
      BigDecimal computedMemoryDurationInMbMinutes,
      String id) {
    repository.updateBillingInfoAttributes(
        status,
        computeDatetime,
        computedDurationInMinutes,
        computedPrice,
        computedMemoryDurationInMbMinutes,
        id);
  }

  public BillingInfo getByQueryId(String queryId) {
    return repository
        .findByQueryId(queryId)
        .orElseThrow(() -> new NotFoundException("Not billing info found for query id " + queryId));
  }

  public Optional<BillingInfo> findBy(String appId, String envId, String id) {
    return repository.findByAppIdAndEnvIdAndId(appId, envId, id);
  }
}
