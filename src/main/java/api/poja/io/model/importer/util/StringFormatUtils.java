package api.poja.io.model.importer.util;

import java.util.StringJoiner;

public class StringFormatUtils {
  public static String formatAssign(String name, String url) {
    return name + " = " + url;
  }

  // fixme: uri(uri) format or raw uri
  public static String formatUri(String uri) {
    return formatDoubleQuoted(uri);
  }

  public static String formatDoubleQuoted(String s) {
    return '"' + s + '"';
  }

  public static String formatSingleQuoted(String s) {
    return '\'' + s + '\'';
  }

  public static StringJoiner formatBlock(String name) {
    return new StringJoiner("\n", name + "{\n", "\n}");
  }

  public static StringJoiner formatAnonBlock() {
    return formatBlock("");
  }
}
