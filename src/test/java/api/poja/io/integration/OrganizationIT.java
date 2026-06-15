package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.OrganizationInviteType.*;
import static api.poja.io.endpoint.rest.model.OrganizationMembersMovementTypeEnum.ADD;
import static api.poja.io.endpoint.rest.model.OrganizationMembersMovementTypeEnum.REMOVE;
import static api.poja.io.integration.conf.utils.TestMocks.INVITE_TO_CANCEL_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_ID;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_2_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_3_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_5_ID;
import static api.poja.io.integration.conf.utils.TestMocks.SUSPENDED_ID;
import static api.poja.io.integration.conf.utils.TestMocks.SUSPENDED_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.janeDoeOrgs;
import static api.poja.io.integration.conf.utils.TestMocks.janeDoeUser;
import static api.poja.io.integration.conf.utils.TestMocks.janeDoeUserResponse;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeAcceptedOrgInvites;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeMainOrg;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeOrg;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeOrgPendingInvites;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeOrganizationSetupStates;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoePendingOrgInvites;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeUser;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeUserResponse;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsNotFoundException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.OrgApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.CrupdateOrganizationMembersRequestBody;
import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.endpoint.rest.model.OrganizationInvite;
import api.poja.io.endpoint.rest.model.UpdateOrganizationInviteRequestBody;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
@AutoConfigureMockMvc
public class OrganizationIT extends MockedThirdParties {

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
  }

  private static Organization ignoreIdAndDatetimeAndMembersCount(Organization organization) {
    return organization.id(null).creationDatetime(null).membersCount(null);
  }

  @Test
  void get_paginated_org_members() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    var expected = List.of(joeDoeUserResponse());

    var actual = orgApi.getOrganizationUsers(ORG_1_ID, 1, 1).getData();

    assertEquals(expected, actual);
  }

  @Test
  void get_user_orgs_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    var orgApi = new OrgApi(janeDoeClient);

    var expected =
        janeDoeOrgs().stream().map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount).toList();

    var actual =
        orgApi.getOrganizations(JANE_DOE_ID, 1, 10).getData().stream()
            .map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount)
            .toList();

    assertTrue(actual.containsAll(expected));
  }

  @Test
  void suspended_get_orgs_ok() {
    ApiClient suspendedUser = anApiClient(SUSPENDED_TOKEN);
    var orgApi = new OrgApi(suspendedUser);

    assertDoesNotThrow(() -> orgApi.getOrganizations(SUSPENDED_ID, 1, 10));
  }

  @Test
  void crupdate_orgs_ok() throws ApiException {
    ApiClient loremIpsumClient = anApiClient(LOREM_IPSUM_TOKEN);
    var orgApi = new OrgApi(loremIpsumClient);

    var expected =
        List.of(new Organization().id("dummy_id").name("dummy_name").ownerId(LOREM_IPSUM_ID));

    var actual =
        orgApi.crupdateOrganizations(LOREM_IPSUM_ID, expected).stream()
            .map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount)
            .toList();

    var actualUserOrgs =
        requireNonNull(orgApi.getOrganizations(LOREM_IPSUM_ID, 1, 10).getData()).stream()
            .map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount)
            .toList();

    // Check org owner has been invited to org
    assertTrue(
        actualUserOrgs.containsAll(
            expected.stream().map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount).toList()));
    assertTrue(
        actual.containsAll(
            expected.stream().map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount).toList()));
  }

  @Test
  void not_e2e_user_cannot_have_more_than_3_orgs() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    var orgApi = new OrgApi(janeDoeClient);

    Organization toCreate =
        new Organization().id("dummy_id_1").name("dummy_name_1").ownerId(JANE_DOE_ID);
    Organization toCreate2 =
        new Organization().id("dummy_id_2").name("dummy_name_2").ownerId(JANE_DOE_ID);

    assertThrowsBadRequestException(
        () -> {
          orgApi.crupdateOrganizations(JANE_DOE_ID, List.of(toCreate, toCreate2));
        },
        "cannot have more than 3 organizations per user");
  }

  @Test
  void invite_user_to_org_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    var orgApi = new OrgApi(janeDoeClient);
    var expected = List.of(joeDoeUser());
    var joeDoeOrg2Movement =
        new CrupdateOrganizationMembersRequestBody().idUser(JOE_DOE_ID).movementType(ADD);

    var actual = orgApi.crupdateOrganizationUsers(ORG_5_ID, List.of(joeDoeOrg2Movement));

    assertEquals(expected, actual);
  }

  @Test
  void remove_self_from_org_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    var orgApi = new OrgApi(janeDoeClient);
    var expected = List.of(janeDoeUser());

    var removeSelfMovement =
        new CrupdateOrganizationMembersRequestBody().idUser(JANE_DOE_ID).movementType(REMOVE);

    var actual = orgApi.crupdateOrganizationUsers(ORG_1_ID, List.of(removeSelfMovement));

    assertEquals(expected, actual);
  }

  @Test
  void remove_user_from_org_ko() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    var orgApi = new OrgApi(janeDoeClient);

    var janeDoeOrg2Movement =
        new CrupdateOrganizationMembersRequestBody().idUser(JANE_DOE_ID).movementType(REMOVE);

    assertThrowsBadRequestException(
        () -> orgApi.crupdateOrganizationUsers(ORG_2_ID, List.of(janeDoeOrg2Movement)),
        "Organization owner cannot be removed");
  }

  @Test
  void get_organization_by_id_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    var expected = ignoreIdAndDatetimeAndMembersCount(joeDoeOrg());

    var actual = ignoreIdAndDatetimeAndMembersCount(orgApi.getOrgnizationById(ORG_1_ID));

    assertEquals(expected, actual);
  }

  @Test
  void get_organization_by_id_ko() {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    assertThrowsNotFoundException(
        () -> orgApi.getOrgnizationById("dummy"), "Organization with id dummy is not found");
  }

  @Test
  void accepted_and_pending_invites_count_in_org_membership_threshold() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    var org1 = orgApi.getOrgnizationById(ORG_1_ID);

    assertEquals(5, org1.getMembersCount());
  }

  @Test
  void pending_and_rejected_invites_do_not_count_as_org_members_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    var actual = orgApi.getOrganizationUsers(ORG_3_ID, 1, 10).getData();

    assertTrue(requireNonNull(actual).contains(joeDoeUserResponse()));
    assertFalse(actual.contains(janeDoeUserResponse()));
  }

  @Test
  void each_user_has_default_main_org() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    var expected = ignoreIdAndDatetimeAndMembersCount(joeDoeMainOrg());

    var actual =
        orgApi.getOrganizations(JOE_DOE_ID, 1, 10).getData().stream()
            .map(OrganizationIT::ignoreIdAndDatetimeAndMembersCount)
            .toList();

    assertTrue(actual.contains(expected));
  }

  @Test
  void get_paginated_user_invites_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    var orgApi = new OrgApi(joeDoeClient);

    var actual = orgApi.getUserOrginvites(JOE_DOE_ID, 1, 10).getData();

    assertFalse(requireNonNull(actual).containsAll(joeDoeAcceptedOrgInvites()));
    assertTrue(requireNonNull(actual).containsAll(joeDoePendingOrgInvites()));
  }

  @Test
  void update_user_org_invite_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    var orgApi = new OrgApi(janeDoeClient);

    var expectedNewOrg =
        new Organization()
            .id("org_4_id")
            .name("org_4_name")
            .creationDatetime(Instant.parse("2025-02-21T00:00:00.00Z"))
            .ownerId(JOE_DOE_ID)
            .membersCount(null);

    var updatedInvite =
        orgApi.updateUserOrganizationInvite(
            JANE_DOE_ID,
            new UpdateOrganizationInviteRequestBody().id("invite_7_id").type(ACCEPTED));

    var actualUserOrgs =
        orgApi.getOrganizations(JANE_DOE_ID, 1, 10).getData().stream()
            .map(a -> a.membersCount(null))
            .toList();

    assertTrue(requireNonNull(actualUserOrgs).contains(expectedNewOrg));
  }

  @Test
  void get_paginated_invitees_suggestions_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    OrgApi api = new OrgApi(joeDoeClient);

    var expected =
        List.of(joeDoeUserResponse().isOrgMember(true), janeDoeUserResponse().isOrgMember(true));

    var actualFilteredByName =
        api.getOrganizationInviteesSuggestions(ORG_1_ID, "j", 1, 10).getData();
    var actualWithEmptyUsername =
        api.getOrganizationInviteesSuggestions(ORG_1_ID, "", 1, 10).getData();
    var actualWithDummyUsername =
        api.getOrganizationInviteesSuggestions(ORG_1_ID, "dummy_username", 1, 10).getData();

    assertTrue(requireNonNull(actualFilteredByName).containsAll(expected));
    assertTrue(requireNonNull(actualWithEmptyUsername).containsAll(expected));
    assertTrue(requireNonNull(actualWithDummyUsername).isEmpty());
  }

  @Test
  void get_paginated_pending_invite_for_org_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    OrgApi api = new OrgApi(joeDoeClient);

    var actual =
        requireNonNull(api.getOrganizationInvites(JOE_DOE_MAIN_ORG_ID, PENDING, 1, 10).getData())
            .stream()
            .map(OrganizationIT::ignoreOrganizationInviteDatetime)
            .toList();

    assertTrue(actual.containsAll(joeDoeOrgPendingInvites()));
  }

  @Test
  void cancel_organization_invite_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    OrgApi janeDoeOrgApi = new OrgApi(janeDoeClient);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    OrgApi joeDoeOrgApi = new OrgApi(joeDoeClient);

    var canceledInvite =
        ignoreOrganizationInviteDatetime(
            janeDoeOrgApi.cancelOrganizationInvite(JANE_DOE_MAIN_ORG_ID, INVITE_TO_CANCEL_ID));

    var joeDoeUpdatedInvites =
        requireNonNull(joeDoeOrgApi.getUserOrginvites(JOE_DOE_ID, 1, 10).getData()).stream()
            .map(OrganizationIT::ignoreOrganizationInviteDatetime)
            .toList();

    assertFalse(joeDoeUpdatedInvites.contains(canceledInvite));
  }

  @Test
  void get_joe_doe_organization_setup_states_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    OrgApi api = new OrgApi(joeDoeClient);

    var states = api.getOrganizationSetupStates(ORG_1_ID).getData();

    assertEquals(joeDoeOrganizationSetupStates(), states);
  }

  private static OrganizationInvite ignoreOrganizationInviteDatetime(OrganizationInvite invite) {
    return invite.invitedAt(null);
  }
}
