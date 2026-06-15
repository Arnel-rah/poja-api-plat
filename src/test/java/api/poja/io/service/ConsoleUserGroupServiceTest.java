package api.poja.io.service;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_USERNAME;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainBadRequestException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.aws.iam.IamComponent;
import api.poja.io.conf.MockedThirdParties;
import api.poja.io.repository.jpa.ConsoleUserGroupRepository;
import api.poja.io.repository.model.ConsoleUserGroup;
import api.poja.io.service.validator.ConsoleUserGroupThresholdValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

class ConsoleUserGroupServiceTest extends MockedThirdParties {
  @Autowired ConsoleUserGroupService subject;
  @MockBean ConsoleUserGroupRepository consoleUserGroupRepository;
  @MockBean IamComponent iamComponent;
  @SpyBean UserService userService;
  @SpyBean UserSubscriptionService userSubscriptionService;
  @SpyBean ConsoleUserGroupThresholdValidator thresholdValidator;

  @Test
  void create_cug_ko() {
    // beta.max.user.orgs.per.user=3
    // max.apps.per.user=2
    // max cug -> 2
    when(consoleUserGroupRepository.countByUserId(JOE_DOE_ID)).thenReturn(Optional.of(2L));

    assertThrowsDomainBadRequestException(
        "cannot create new console user group.",
        () ->
            subject.createNewByUser(
                JOE_DOE_ID,
                JOE_DOE_USERNAME,
                ConsoleUserGroup.builder()
                    .id("new_cug1_id")
                    .userId(JOE_DOE_ID)
                    .orgId(JOE_DOE_MAIN_ORG_ID)
                    .available(true)
                    .computeStackResources(List.of())
                    .build()));

    verify(userService, atLeastOnce()).getUserById(JOE_DOE_ID);
    verify(userSubscriptionService, times(1)).findActiveSubscriptionByUserId(JOE_DOE_ID);
    verify(iamComponent, never()).createGroupAndAttachUserToGroup(any(), any());
  }

  @Test
  void create_cug_ok() {
    when(consoleUserGroupRepository.countByUserId(JOE_DOE_ID)).thenReturn(Optional.of(1L));

    subject.createNewByUser(
        JOE_DOE_ID,
        JOE_DOE_USERNAME,
        ConsoleUserGroup.builder()
            .id("new_cug2_id")
            .userId(JOE_DOE_ID)
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .available(true)
            .computeStackResources(List.of())
            .build());

    verify(userService, atLeastOnce()).getUserById(JOE_DOE_ID);
    verify(userSubscriptionService, times(1)).findActiveSubscriptionByUserId(JOE_DOE_ID);
    verify(thresholdValidator, times(1)).accept(JOE_DOE_ID);
    verify(iamComponent, times(1)).createGroupAndAttachUserToGroup(any(), any());
  }
}
