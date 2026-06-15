package api.poja.io.endpoint.rest.controller.admin;

import static api.poja.io.model.UserStatus.ACTIVE;
import static api.poja.io.model.UserStatus.SUSPENDED;
import static java.util.Objects.requireNonNull;

import api.poja.io.endpoint.rest.mapper.UserBillingDiscountMapper;
import api.poja.io.endpoint.rest.mapper.UserMapper;
import api.poja.io.endpoint.rest.model.GetUserBillingDiscountResponse;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.UpdateUserStatusRequestBody;
import api.poja.io.endpoint.rest.model.User;
import api.poja.io.endpoint.rest.model.UserBillingDiscount;
import api.poja.io.model.UserStatus;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import api.poja.io.service.UserBillingDiscountService;
import api.poja.io.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AdminUserController {
  private final UserService userService;
  private final UserMapper userMapper;
  private final UserBillingDiscountService discountService;
  private final UserBillingDiscountMapper discountMapper;

  @DeleteMapping("/users/{id}")
  public User deleteUser(@PathVariable String id) {
    return userMapper.toRest(userService.archiveUser(id));
  }

  @PutMapping("/users/{userId}/billing-discounts")
  public UserBillingDiscount grantUserDiscount(
      @PathVariable String userId, @RequestBody UserBillingDiscount userBillingDiscount) {
    return discountMapper.toRest(discountService.save(userId, userBillingDiscount));
  }

  @GetMapping("/users/{userId}/billing-discounts")
  public GetUserBillingDiscountResponse getUserBillingDiscounts(
      @PathVariable String userId,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) MonthType month) {
    return new GetUserBillingDiscountResponse()
        .data(
            discountService
                .findAllByUserIdAndYearAndMonth(
                    userId, year, PaymentRequestPeriod.valueOf(month.name()))
                .stream()
                .map(discountMapper::toRest)
                .toList());
  }

  @PutMapping("/users/{id}/statuses")
  public User updateUserStatus(
      @PathVariable String id, @RequestBody UpdateUserStatusRequestBody requestBody) {
    return userMapper.toRest(
        userService.updateUserStatusAsync(
            id, getStatusEnum(requireNonNull(requestBody.getAction())), requestBody.getReason()));
  }

  private static UserStatus getStatusEnum(UpdateUserStatusRequestBody.ActionEnum action) {
    return switch (action) {
      case ACTIVATE -> ACTIVE;
      case SUSPEND -> SUSPENDED;
    };
  }
}
