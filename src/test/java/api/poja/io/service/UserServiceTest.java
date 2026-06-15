package api.poja.io.service;

import static api.poja.io.integration.conf.utils.TestMocks.joeDoeUser;
import static api.poja.io.model.UserStatus.ACTIVE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.UserCreated;
import api.poja.io.endpoint.event.model.UserSubscriptionRequested;
import api.poja.io.endpoint.rest.model.CreateUser;
import api.poja.io.model.User;
import api.poja.io.repository.UserRepository;
import api.poja.io.repository.model.UserState;
import api.poja.io.service.workflows.userState.UserStateService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHMyself;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

class UserServiceTest extends MockedThirdParties {

  static final String MOCK_USER_ID = "mock_user_id";

  @Autowired private UserService subject;
  @SpyBean private UserRepository userRepositorySpy;
  @MockBean private UserStateService userStateServiceMock;

  @BeforeEach()
  void setUp() {
    when(userStateServiceMock.save(eq(MOCK_USER_ID), any(), any(), any()))
        .thenReturn(UserState.builder().userId(MOCK_USER_ID).progressionStatus(ACTIVE).build());
    when(userRepositorySpy.findById(eq(MOCK_USER_ID), any()))
        .thenReturn(Optional.of(User.builder().id(MOCK_USER_ID).email("noob@bot.com").build()));
    when(userRepositorySpy.saveAll(anyList()))
        .thenAnswer(
            invocation -> {
              var list = (List<User>) invocation.getArgument(0);
              return list.stream()
                  .map(u -> u.toBuilder().id(MOCK_USER_ID).githubId("1010101").build())
                  .toList();
            });
  }

  @Test
  void lastConnection_shouldBe_updated_when_stale() {
    var user = user_with_lastConnection();
    var now = Instant.parse("2024-10-01T12:20:00Z"); // 20mn diff

    subject.updateLastConnection(user, now);

    verify(userRepositorySpy, times(1)).updateLastConnection(user.getId(), now);
    verifyNoMoreInteractions(userRepositorySpy);
  }

  @Test
  void lastConnection_shouldNotBe_updated_when_recent() {
    var user = user_with_lastConnection();
    var now = Instant.parse("2024-10-01T12:05:00Z"); // 5mn diff

    subject.updateLastConnection(user, now);

    verifyNoMoreInteractions(userRepositorySpy);
  }

  @Test
  void lastConnection_shouldNotBe_updated_when_now_isBefore_lastConnection() {
    var user = user_with_lastConnection();
    var now = Instant.parse("2024-10-01T10:00:00Z"); // -2hr

    subject.updateLastConnection(user, now);

    verifyNoMoreInteractions(userRepositorySpy);
  }

  @SneakyThrows
  @Test
  void newly_created_user_shouldBe_notified() {
    var myToken = randomUUID().toString();

    GHMyself gh = mocked_ghMyself();
    doReturn(Optional.of(gh)).when(githubComponentMock).getCurrentUserByToken(myToken);

    subject.createUsers(List.of(new CreateUser().email("noob@bot.com").token(myToken)));

    verify(eventProducerMock, times(1))
        .accept(
            assertArg(
                evs -> {
                  var evOpt = evs.stream().filter(e -> e instanceof UserCreated).findFirst();
                  assertTrue(evOpt.isPresent());
                  var ev = (UserCreated) evOpt.get();
                  assertEquals(MOCK_USER_ID, ev.getUserId());
                }));
  }

  @SneakyThrows
  @Test
  void should_subscribe_user_on_creation_for_idp() {

    when(platformConfSpy.isSaas()).thenReturn(false);
    when(platformConfSpy.isIdp()).thenReturn(true);

    var myToken = randomUUID().toString();

    var gh = mocked_ghMyself();
    doReturn(Optional.of(gh)).when(githubComponentMock).getCurrentUserByToken(myToken);

    subject.createUsers(List.of(new CreateUser().email("noob@bot.com").token(myToken)));

    verify(eventProducerMock, times(1))
        .accept(
            assertArg(
                evs -> {
                  var evOpt =
                      evs.stream().filter(e -> e instanceof UserSubscriptionRequested).findFirst();
                  assertTrue(evOpt.isPresent());
                  var ev = (UserSubscriptionRequested) evOpt.get();
                  assertEquals(MOCK_USER_ID, ev.getUserId());
                }));
  }

  @SneakyThrows
  @Test
  void should_not_subscribe_user_on_creation_for_saas() {

    when(platformConfSpy.isSaas()).thenReturn(true);
    when(platformConfSpy.isIdp()).thenReturn(false);

    var myToken = randomUUID().toString();

    var gh = mocked_ghMyself();
    doReturn(Optional.of(gh)).when(githubComponentMock).getCurrentUserByToken(myToken);

    subject.createUsers(List.of(new CreateUser().email("noob@bot.com").token(myToken)));

    verify(eventProducerMock, times(1))
        .accept(
            assertArg(
                evs -> {
                  var evOpt =
                      evs.stream().filter(e -> e instanceof UserSubscriptionRequested).findFirst();
                  assertTrue(evOpt.isEmpty());
                }));
  }

  @SneakyThrows
  private static GHMyself mocked_ghMyself() {
    GHMyself ghMock = mock();
    doReturn("noob@bot.com").when(ghMock).getEmail();
    doReturn("noob").when(ghMock).getName();
    doReturn(1010101L).when(ghMock).getId();

    return ghMock;
  }

  private static User user_with_lastConnection() {
    // partial user model
    var user = new User();
    user.setId(joeDoeUser().getId());
    user.setLastConnection(Instant.parse("2024-10-01T12:05:00Z"));
    return user;
  }
}
