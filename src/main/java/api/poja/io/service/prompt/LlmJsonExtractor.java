package api.poja.io.service.prompt;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class LlmJsonExtractor {

    private LlmJsonExtractor() {}

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*\\]", Pattern.DOTALL);
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("```(?:json)?\\s*(.*?)```", Pattern.DOTALL);

    public static String extractJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("LLM response is null or empty");
        }

        String cleaned = cleanResponse(response);

        String json = extractFirstMatch(cleaned, JSON_OBJECT_PATTERN)
                .or(() -> extractFirstMatch(cleaned, JSON_ARRAY_PATTERN))
                .orElseThrow(() -> new IllegalArgumentException("No valid JSON object or array found in LLM response"));

        log.debug("Extracted JSON ({} characters)", json.length());
        return json;
    }

    private static String cleanResponse(String response) {
        String cleaned = CODE_FENCE_PATTERN.matcher(response).replaceAll("$1");

        cleaned = cleaned
                .replaceAll("(?i)```json", "")
                .replaceAll("(?i)```", "")
                .replaceAll("(?i)json\\s*:\\s*", "")
                .replaceAll("(?s)<thinking>.*?</thinking>", "")
                .replaceAll("(?s)Thought:.*?(?=\\{)", "")
                .trim();

        return cleaned;
    }

    private static java.util.Optional<String> extractFirstMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String match = matcher.group(0);
            if (isLikelyValidJson(match)) {
                return java.util.Optional.of(match);
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean isLikelyValidJson(String str) {
        if (str == null || str.length() < 2) return false;
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}