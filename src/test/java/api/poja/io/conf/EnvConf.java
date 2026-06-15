package api.poja.io.conf;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.sys.platform.PlatformConf.Mode.SAAS;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {
  private static final String NETWORKING_CONFIG_STRING_VALUE =
      "{\"region\": \"eu-west-3\",\"with_own_vpc\": true, \"ssm_sg_id\": \"sg-id\","
          + " \"ssm_subnet1_id\": \"subnet-1\", \"ssm_subnet2_id\": \"subnet-2\"}";
  public static final String BETA_USERS =
      """
      ["JoeDoe", "JaneDoe", "Admin", "Suspended", "LoremIpsum", "Archived", "Noobie"]
      """;
  public static final int BETA_USER_ALLOWED_APP_NB = 5;

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.locations", () -> "classpath:/db/migration,classpath:/db/testdata");
    registry.add("github.client.id", () -> "dummy");
    registry.add("github.client.secret", () -> "dummy");
    registry.add("github.redirect.uri", () -> "dummy");
    registry.add("github.token.url", () -> "dummy");
    registry.add("apps.envs.networking", () -> NETWORKING_CONFIG_STRING_VALUE);
    registry.add("github.api.baseuri", () -> "https://api.github.com");
    registry.add("poja.sam.api.key", () -> "dummy");
    registry.add("poja.sam.api.uri", () -> "https://cligenpoja.com");
    registry.add("stripe.api.key", () -> "dummy");
    registry.add("env", () -> "test");
    registry.add("beta.users", () -> BETA_USERS);
    registry.add("private.beta.test", () -> true);
    registry.add("aws.account.id", () -> "01");
    registry.add("pricing.free.tier", () -> 2);
    registry.add("max.apps.per.user", () -> 2);
    registry.add("beta.user.console.threshold", () -> 5);
    registry.add("beta.user.app.threshold", () -> BETA_USER_ALLOWED_APP_NB);
    registry.add("beta.max.user.orgs.per.user", () -> 3);
    registry.add("max.groups.per.user", () -> 1);
    registry.add("max.groups.per.org", () -> 1);
    registry.add("max.orgs.per.user", () -> 1);
    registry.add("max.users.per.org", () -> 10);
    registry.add("max.premium.subscribers", () -> 1);
    registry.add("max.org.memberships.per.user", () -> 5);
    registry.add("e2e.max.apps.per.user", () -> 8);
    registry.add("e2e.max.groups.per.user", () -> 2);
    registry.add("e2e.max.orgs.per.user", () -> 1);
    registry.add("e2e.user.id", () -> JOE_DOE_ID);
    registry.add("e2e.user.main.org.id", () -> JOE_DOE_MAIN_ORG_ID);
    registry.add("user.suspension.grace.period", () -> 5);
    registry.add("max.allowed.inactivity.days", () -> 60);
    registry.add("org.app.invitation.base.url", () -> "dummy");
    registry.add("poja.console.base.url", () -> "dummy");
    registry.add("app.pem.bucket.key", () -> "dummy");
    registry.add("mailjet.api.sender.email", () -> "contact@poja.io");
    registry.add("mailjet.api.key", () -> "dummy");
    registry.add("mailjet.api.secret", () -> "dummy");
    registry.add("mailjet.templateid.accountcreated", () -> 1L);
    registry.add("platform.mode", () -> SAAS);
    registry.add("exchange.rate.eur.usd", () -> 1.1535);
    registry.add("exchange.rate.usd.eur", () -> 0.8672);
    registry.add("target.account.execution.role.arn", () -> "dummy");
  }
}
