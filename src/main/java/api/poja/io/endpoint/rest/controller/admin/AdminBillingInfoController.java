package api.poja.io.endpoint.rest.controller.admin;

import static api.poja.io.model.RangedInstant.getRangedInstant;

import api.poja.io.endpoint.rest.mapper.BillingInfoMapper;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.PagedUsersBillingInfoResponse;
import api.poja.io.endpoint.rest.model.SortBy;
import api.poja.io.endpoint.rest.model.SortOrder;
import api.poja.io.endpoint.rest.model.UserBillingInfoWithAws;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.RangedInstant;
import api.poja.io.model.page.Page;
import api.poja.io.service.BillingInfoService;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AdminBillingInfoController {
  private final BillingInfoMapper mapper;
  private BillingInfoService service;

  @GetMapping("/billings")
  public PagedUsersBillingInfoResponse getUsersBillingInfo(
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "10", name = "page_size")
          BoundedPageSize pageSize,
      @RequestParam(required = false) Instant startTime,
      @RequestParam(required = false) Instant endTime,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) MonthType month,
      @RequestParam(required = false) String username,
      @RequestParam(required = false, name = "archived") Boolean archived,
      @RequestParam(required = false) SortBy sortBy,
      @RequestParam(required = false) SortOrder sortOrder,
      @RequestParam(required = false) Instant joinedFrom,
      @RequestParam(required = false) Instant joinedTo) {
    RangedInstant datetimeRange = getRangedInstant(startTime, endTime, year, month);
    var joinedRange = new RangedInstant(joinedFrom, joinedTo);

    Page<UserBillingInfoWithAws> data =
        service
            .getUsersBillingInfo(
                page, pageSize, datetimeRange, archived, username, sortBy, sortOrder, joinedRange)
            .map(b -> mapper.toUserRest(b, datetimeRange.start(), datetimeRange.end()));
    return new PagedUsersBillingInfoResponse()
        .pageNumber(data.queryPage().getValue())
        .pageSize(data.queryPageSize().getValue())
        .hasPrevious(data.hasPrevious())
        .count(data.count())
        .data(data.data().stream().toList());
  }
}
