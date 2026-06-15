package api.poja.io.model.importer.analyzer.lang;

import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import api.poja.io.endpoint.EndpointConf;
import api.poja.io.model.importer.model.UnknownApplication;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppLanguageAnalyzerTest {
  public static final String JAVA_PROJECT_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/java_project";
  public static final String NOT_JAVA_PROJECT_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/react_project";
  public static final String MAIN_NOT_STATIC_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/main_not_static";
  public static final String MAIN_NOT_PUBLIC_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/main_not_public";
  public static final String MAIN_NOT_VOID_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/main_not_void";
  public static final String MAIN_WEIRD_MODIFIERS_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/main_weird_modifiers";
  public static final String MAIN_MULTILINE_WITH_COMMENTS_RESOURCE_PATH =
      "files/import/analyzer/lang/project_mock/main_multiline_with_comments";
  public static final String MAIN_WITH_ANNOTATIONS =
      "files/import/analyzer/lang/project_mock/main_with_annotations";
  private final AppLanguageAnalyzer subject = new AppLanguageAnalyzer();

  @Test
  void should_succeed_when_project_is_java() throws IOException {
    var app = new UnknownApplication(getFile(JAVA_PROJECT_RESOURCE_PATH), null);

    var result = subject.analyze(app);
    AppLangAnalyzerData data = result.data();

    assertEquals(SUCCESS, result.status());
    assertEquals(Path.of("src/main/java/demo/example/JavaApp.java"), data.mainClassPath());
  }

  @Test
  void should_succeed_when_main_method_is_multiline_with_comments() throws IOException {
    var app = new UnknownApplication(getFile(MAIN_MULTILINE_WITH_COMMENTS_RESOURCE_PATH), null);

    var result = subject.analyze(app);
    AppLangAnalyzerData data = result.data();

    assertEquals(SUCCESS, result.status());
    assertEquals(Path.of("src/main/java/JavaApp.java"), data.mainClassPath());
  }

  @Test
  void should_succeed_when_modifiers_are_in_weird_order() throws IOException {
    var app = new UnknownApplication(getFile(MAIN_WEIRD_MODIFIERS_RESOURCE_PATH), null);

    var result = subject.analyze(app);
    AppLangAnalyzerData data = result.data();

    assertEquals(SUCCESS, result.status());
    assertEquals(Path.of("src/main/java/JavaApp.java"), data.mainClassPath());
  }

  @Test
  void should_succeed_when_main_method_has_annotations() throws IOException {
    var app = new UnknownApplication(getFile(MAIN_WITH_ANNOTATIONS), null);

    var result = subject.analyze(app);
    AppLangAnalyzerData data = result.data();

    assertEquals(SUCCESS, result.status());
    assertEquals(Path.of("src/main/java/JavaApp.java"), data.mainClassPath());
  }

  @Test
  void should_fail_when_project_is_not_java() {
    var app = new UnknownApplication(getFile(NOT_JAVA_PROJECT_RESOURCE_PATH), null);

    var actual = subject.analyze(app);

    assertEquals(FAILED, actual.status());
    assertNull(actual.data().mainClassPath());
  }

  @Test
  void should_fail_when_main_method_is_not_public() {
    var app = new UnknownApplication(getFile(MAIN_NOT_PUBLIC_RESOURCE_PATH), null);

    var actual = subject.analyze(app);

    assertEquals(FAILED, actual.status());
    assertNull(actual.data().mainClassPath());
  }

  @Test
  void should_fail_when_main_method_is_not_static() {
    var app = new UnknownApplication(getFile(MAIN_NOT_STATIC_RESOURCE_PATH), null);

    var actual = subject.analyze(app);

    assertEquals(FAILED, actual.status());
    assertNull(actual.data().mainClassPath());
  }

  @Test
  void should_fail_when_main_method_is_not_void() {
    var app = new UnknownApplication(getFile(MAIN_NOT_VOID_RESOURCE_PATH), null);

    var actual = subject.analyze(app);

    assertEquals(FAILED, actual.status());
    assertNull(actual.data().mainClassPath());
  }

  @Test
  void resultData_canBe_serialized_and_deserialized() throws IOException {
    var om = new EndpointConf().objectMapper();
    var app = new UnknownApplication(getFile(JAVA_PROJECT_RESOURCE_PATH), null);

    var result = subject.analyze(app);
    AppLangAnalyzerData data = result.data();
    var se = om.writeValueAsString(data);
    var dese = om.readValue(se, AppLangAnalyzerData.class);

    assertEquals(data, dese);
    assertEquals(Path.of("src/main/java/demo/example/JavaApp.java"), dese.mainClassPath());
  }
}
