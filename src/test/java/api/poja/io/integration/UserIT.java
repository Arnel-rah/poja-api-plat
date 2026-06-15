package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.SortOrder.ASC;
import static api.poja.io.endpoint.rest.model.SortOrder.DESC;
import static api.poja.io.endpoint.rest.model.SortUsersBy.JOIN_DATE;
import static api.poja.io.endpoint.rest.model.SortUsersBy.LAST_CONNECTION;
import static api.poja.io.endpoint.rest.model.SortUsersBy.USERNAME;
import static api.poja.io.endpoint.rest.model.UpdateUserStatusRequestBody.ActionEnum.ACTIVATE;
import static api.poja.io.endpoint.rest.model.UpdateUserStatusRequestBody.ActionEnum.SUSPEND;
import static api.poja.io.endpoint.rest.model.UserStatusEnum.ACTIVE;
import static api.poja.io.integration.conf.utils.TestMocks.*;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsUnauthorizedException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static api.poja.io.integration.conf.utils.TestUtils.setupJoeDoeGithubUser;
import static api.poja.io.integration.conf.utils.TestUtils.setupSuspendedGithubUser;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.SecurityApi;
import api.poja.io.endpoint.rest.api.UserApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.CreateUser;
import api.poja.io.endpoint.rest.model.CreateUsersRequestBody;
import api.poja.io.endpoint.rest.model.UpdateUserStatusRequestBody;
import api.poja.io.endpoint.rest.model.User;
import api.poja.io.endpoint.rest.model.Whoami;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHMyself;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class UserIT extends MockedThirdParties {
  @SpyBean private UserRepository userRepositorySpy;

  @BeforeEach
  void setUp() {
    doNothing().when(userRepositorySpy).updateLastConnection(anyString(), any(Instant.class));
  }

  private static Whoami joeDoeWhoami() {
    return new Whoami().user(joeDoeUser());
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @Test
  void whoami_ok() throws ApiException {
    var githubUser = mock(GHMyself.class);
    setupJoeDoeGithubUser(githubUser);
    setUpGithub(githubComponentMock, githubUser);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    SecurityApi api = new SecurityApi(joeDoeClient);

    Whoami actual = api.whoami();

    assertEquals(joeDoeWhoami(), actual);
  }

  @Test
  void whoami_bad_token_ko() {
    setUpGithub(githubComponentMock);
    ApiClient badTokenClient = anApiClient(BAD_TOKEN);
    SecurityApi api = new SecurityApi(badTokenClient);

    assertThrowsUnauthorizedException(api::whoami, "Bad credentials");
  }

  @Test
  void whoami_not_existing_account_for_token_ko() {
    setUpGithub(githubComponentMock);
    ApiClient noMatchingAccountInDbClient = anApiClient(NO_MATCHING_DB_ACCOUNT_TOKEN);
    SecurityApi api = new SecurityApi(noMatchingAccountInDbClient);

    assertThrowsUnauthorizedException(api::whoami, "username not found");
  }

  @Test
  void get_stats_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    UserApi api = new UserApi(joeDoeClient);

    var userStats = api.getUserStatistics();
    assertEquals(3, userStats.getSuspendedUsersNb());
    assertEquals(2, userStats.getArchivedUsersNb());
    assertEquals(17, userStats.getUsersCount());
  }

  @Test
  void suspended_whoami_ok() {
    var githubUser = mock(GHMyself.class);
    setupSuspendedGithubUser(githubUser);
    setUpGithub(githubComponentMock, githubUser);
    ApiClient suspendedClient = anApiClient(SUSPENDED_TOKEN);
    SecurityApi api = new SecurityApi(suspendedClient);

    assertDoesNotThrow(api::whoami);
  }

  @Test
  void signup_ok() throws ApiException {
    var githubUser = mock(GHMyself.class);
    when(githubComponentMock.getGithubUserId(NEW_USER_TOKEN))
        .thenReturn(Optional.of(NEW_USER_GITHUB_ID));
    when(githubComponentMock.getCurrentUserByToken(NEW_USER_TOKEN))
        .thenReturn(Optional.of(githubUser));
    when(githubUser.getLogin()).thenReturn("new_user_github_username");
    when(githubUser.getId()).thenReturn(Long.valueOf(NEW_USER_GITHUB_ID));
    when(githubUser.getAvatarUrl()).thenReturn(JOE_DOE_AVATAR);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    UserApi api = new UserApi(joeDoeClient);

    CreateUser toCreate =
        new CreateUser()
            .firstName("firstName")
            .lastName("lastName")
            .email("test@example.com")
            .token(NEW_USER_TOKEN);

    User actual =
        requireNonNull(
                api.createUser(new CreateUsersRequestBody().data(List.of(toCreate))).getData())
            .getFirst();

    assertEquals("test@example.com", actual.getEmail());
    assertEquals(JOE_DOE_AVATAR, actual.getAvatar());
    assertEquals(NEW_USER_GITHUB_ID, actual.getGithubId());
    assertEquals("new_user_github_username", actual.getUsername());
    assertEquals(ACTIVE, actual.getStatus());
    assertNotNull(actual.getStatusUpdatedAt());
    assertNotNull(actual.getStatusCheckedAt());
  }

  @Test
  void suspended_read_states_ok() {
    setUpGithub(githubComponentMock);
    ApiClient suspendedUserClient = anApiClient(SUSPENDED_TOKEN);
    UserApi api = new UserApi(suspendedUserClient);

    assertDoesNotThrow(() -> api.getUserStates(SUSPENDED_ID));
  }

  @Test
  void user_read_self_states_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    var noobieClient = anApiClient(NOOBIE_TOKEN);
    UserApi api = new UserApi(noobieClient);

    var expectedStates = noobieStates();
    var actualStates = requireNonNull(api.getUserStates(NOOBIE_ID).getData());

    assertEquals(expectedStates.size(), actualStates.size());
    assertEquals(expectedStates, actualStates);
  }

  @Test
  void admin_read_user_states_ok() {
    setUpGithub(githubComponentMock);
    var adminClient = anApiClient(ADMIN_TOKEN);
    UserApi api = new UserApi(adminClient);

    assertDoesNotThrow(() -> api.getUserStates(JOE_DOE_ID));
  }

  @Test
  void joe_doe_read_jane_states_ko() {
    setUpGithub(githubComponentMock);
    var joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    UserApi api = new UserApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getUserStates(JANE_DOE_ID), "Access Denied");
  }

  @Test
  void get_paginated_users_by_username_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    UserApi api = new UserApi(joeDoeClient);

    var expected = List.of(joeDoeUserResponse(), janeDoeUserResponse());

    var actualFilteredByName = api.getUsers("j", null, null, 1, 15).getData();

    var actualWithEmptyUsername = api.getUsers("", null, null, 1, 20).getData();
    var joeDoe = api.getUsers("joe", null, null, 1, 10).getData();

    var sortedByJoinDateDesc = api.getUsers(null, JOIN_DATE, DESC, 1, 500);
    var sortedByJoinDateAsc = api.getUsers(null, JOIN_DATE, ASC, 1, 500);

    var sortedByLastConnectionDesc = api.getUsers(null, LAST_CONNECTION, DESC, 1, 500);
    var sortedByLastConnectionAsc = api.getUsers(null, LAST_CONNECTION, ASC, 1, 500);

    var sortedByUsernameDesc = api.getUsers(null, USERNAME, DESC, 1, 500);
    var sortedByUsernameAsc = api.getUsers(null, USERNAME, ASC, 1, 500);

    assertEquals(joeDoeUserResponse(), joeDoe.getFirst());
    assertTrue(requireNonNull(actualFilteredByName).containsAll(expected));
    assertTrue(requireNonNull(actualWithEmptyUsername).containsAll(expected));

    assertEquals("denis_ritchie_id", sortedByJoinDateDesc.getData().get(0).getId());
    assertEquals("lorem_ipsum_id", sortedByJoinDateDesc.getData().get(1).getId());
    assertEquals("to_suspend_id", sortedByJoinDateDesc.getData().get(2).getId());

    assertEquals("recsus_id", sortedByJoinDateAsc.getData().get(0).getId());
    assertEquals("admin_id", sortedByJoinDateAsc.getData().get(1).getId());
    assertEquals("noobie_id", sortedByJoinDateAsc.getData().get(2).getId());

    assertEquals("recsus_id", sortedByLastConnectionDesc.getData().get(0).getId());
    assertEquals("admin_id", sortedByLastConnectionDesc.getData().get(1).getId());
    assertEquals("denis_ritchie_id", sortedByLastConnectionDesc.getData().get(2).getId());

    assertEquals("lorem_ipsum_id", sortedByLastConnectionAsc.getData().get(0).getId());
    assertEquals("noobie_id", sortedByLastConnectionAsc.getData().get(1).getId());
    assertEquals("to_suspend_id", sortedByLastConnectionAsc.getData().get(2).getId());

    assertEquals("to_upsert_id_2", sortedByUsernameDesc.getData().get(0).getId());
    assertEquals("to_upsert_id", sortedByUsernameDesc.getData().get(1).getId());
    assertEquals("suspended_4_id", sortedByUsernameDesc.getData().get(2).getId());

    assertEquals("to_activate_id", sortedByUsernameAsc.getData().get(0).getId());
    assertEquals("admin_id", sortedByUsernameAsc.getData().get(1).getId());
    assertEquals("to_archive_id", sortedByUsernameAsc.getData().get(2).getId());
  }

  @Test
  void get_user_by_id_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    UserApi api = new UserApi(joeDoeClient);

    var actual = api.getUserById(JOE_DOE_ID);

    assertEquals(joeDoeUserResponse(), actual);
  }

  @Test
  void delete_user_by_id_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient adminClient = anApiClient(ADMIN_TOKEN);
    UserApi api = new UserApi(adminClient);

    api.deleteUser("to_archive_id");
  }

  @Test
  void suspend_user_by_id_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient adminClient = anApiClient(ADMIN_TOKEN);
    UserApi api = new UserApi(adminClient);

    String reason =
        "Your account has been temporarily suspended due to repeated deployment of applications"
            + " that violate our Acceptable Use Policy.";

    api.updateUserStatus(
        "to_suspend_id", new UpdateUserStatusRequestBody().action(SUSPEND).reason(reason));
  }

  @Test
  void activate_user_by_id_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient adminClient = anApiClient(ADMIN_TOKEN);
    UserApi api = new UserApi(adminClient);

    api.updateUserStatus("to_activate_id", new UpdateUserStatusRequestBody().action(ACTIVATE));
  }

  @Test
  void archived_whoami_ko() {
    setUpGithub(githubComponentMock);
    ApiClient apiClient = anApiClient(ARCHIVED_TOKEN);
    SecurityApi api = new SecurityApi(apiClient);

    assertThrowsUnauthorizedException(api::whoami, "User account has been deactivated");
  }

  @Test
  void cannot_suspend_user_with_the_same_reason_within_grace_period() {
    setUpGithub(githubComponentMock);
    ApiClient adminClient = anApiClient(ADMIN_TOKEN);
    UserApi api = new UserApi(adminClient);

    assertThrowsBadRequestException(
        () ->
            api.updateUserStatus(
                "recsus_id",
                new UpdateUserStatusRequestBody()
                    .action(SUSPEND)
                    .reason("admin: first suspension")),
        "User.id=recsus_id cannot be suspended for the same reason again until 3 days.");
  }

  @Test
  void get_user_payment_setup_states_ok() throws ApiException {
    setUpGithub(githubComponentMock);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    UserApi api = new UserApi(joeDoeClient);

    var actual = api.getUserPaymentSetupStates(JOE_DOE_ID).getData();

    assertEquals(joeDoePaymentSetupStates(), actual);
  }
}
