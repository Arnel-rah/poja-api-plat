package api.poja.io.model.importer.transformer.utils;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static javax.xml.transform.OutputKeys.ENCODING;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class Xmls {

  private static final Transformer transformer;

  static {
    try {
      transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(INDENT, "yes");
      transformer.setOutputProperty(ENCODING, "UTF-8");
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  // prevent instantiation; this class is purely static
  private Xmls() {}

  public static Document parseXml(File file) {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringComments(true);
      factory.setNamespaceAware(false);
      var builder = factory.newDocumentBuilder();
      return builder.parse(file);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new RuntimeException(e);
    }
  }

  public static String printXml(Element element) {
    var sw = new StringWriter();
    try {
      // note: avoid mutating the provided `Element`
      var clonedElement = element.cloneNode(true);
      removeWhitespaceNodes(clonedElement);
      transformer.transform(new DOMSource(clonedElement), new StreamResult(sw));
      var xml = sw.toString().trim();
      return normalizeNewlines(xml);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  /** note: remove when we are able to set the line separator for xml `Transformer` */
  private static String normalizeNewlines(String s) {
    return s.replaceAll("\\r\\n", "\n");
  }

  public static void removeWhitespaceNodes(Node node) {
    NodeList children = node.getChildNodes();
    for (int i = children.getLength() - 1; i >= 0; i--) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) {
        node.removeChild(child);
      } else if (child.getNodeType() == Node.ELEMENT_NODE) {
        removeWhitespaceNodes(child);
      }
    }
  }

  /** Retrieves the direct child element inside the `parent` that has the provided `tag` */
  public static Optional<Element> findElement(Element parent, String tag) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == ELEMENT_NODE && tag.equals(node.getNodeName())) {
        return Optional.of((Element) node);
      }
    }
    return Optional.empty();
  }

  /** Retrieves the direct child element inside the `parent` that has the provided `tag` */
  public static List<Element> getChildElements(Element parent, String tag) {
    NodeList children = parent.getChildNodes();
    List<Element> elements = new ArrayList<>();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == ELEMENT_NODE && tag.equals(node.getNodeName())) {
        elements.add((Element) node);
      }
    }
    return elements;
  }

  public static void removeElement(Element parent, String tag) {
    var elementOpt = findElement(parent, tag);
    elementOpt.ifPresent(parent::removeChild);
  }

  public static void removeIfEmpty(Element element) {
    if (!hasChildElements(element)) {
      element.getParentNode().removeChild(element);
    }
  }

  public static boolean hasChildElements(Element element) {
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i).getNodeType() == ELEMENT_NODE) return true;
    }
    return false;
  }

  /**
   * Retrieves the text content of the first direct child element with the given {@code tag}, then
   * removes that element from the {@code parent}.
   *
   * <p>This method both reads and consumes the tag — the matched element will no longer exist in
   * the parent after the call.
   */
  public static @Nullable String text(Element parent, String tag) {
    var elementOpt = findElement(parent, tag);
    var textContent = elementOpt.map(e -> e.getTextContent().trim()).orElse(null);
    elementOpt.ifPresent(parent::removeChild);
    return textContent;
  }

  /**
   * Same as {@link #text(Element, String)}, but returns the provided {@code defaultValue} if no
   * matching element is found or if its text content is empty.
   */
  public static String textOrDefault(Element parent, String tag, String defaultValue) {
    var elementOpt = findElement(parent, tag);
    var textContent = elementOpt.map(e -> e.getTextContent().trim()).orElse(defaultValue);
    elementOpt.ifPresent(parent::removeChild);
    return textContent;
  }

  public static Set<String> collectTagPaths(Element root) {
    List<String> paths = new ArrayList<>();
    collectRemainingTagPathsRecursive(root, "", paths);
    return paths.stream().collect(toUnmodifiableSet());
  }

  private static void collectRemainingTagPathsRecursive(
      Element element, String prefix, List<String> paths) {
    NodeList children = element.getChildNodes();

    if (children.getLength() == 0) {
      return;
    }

    String currentPath =
        prefix.isEmpty() ? element.getTagName() : prefix + "/" + element.getTagName();
    paths.add(currentPath);
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child.getNodeType() == ELEMENT_NODE) {
        collectRemainingTagPathsRecursive((Element) child, currentPath, paths);
      }
    }
  }

  /** Returns only the deepest tag paths, removing any path that is a parent of another. */
  public static List<String> retainDeepestTagPaths(Collection<String> paths) {
    return paths.stream()
        .filter(
            p -> paths.stream().noneMatch(other -> !other.equals(p) && other.startsWith(p + "/")))
        .toList();
  }
}
