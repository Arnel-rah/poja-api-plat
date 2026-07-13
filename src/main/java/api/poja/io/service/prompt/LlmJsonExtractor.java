package api.poja.io.service.prompt;

public final class LlmJsonExtractor {

    private LlmJsonExtractor() {
    }

    public static String extractJsonObject(String response) {
        if (response == null) {
            throw new IllegalArgumentException("Empty LLM response");
        }

        String cleaned = response
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf('{');
        if (start == -1) {
            throw new IllegalArgumentException("No JSON object found in LLM response");
        }

        int depth = 0;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{') depth++;
            if (c == '}') depth--;
            if (depth == 0) {
                return cleaned.substring(start, i + 1);
            }
        }

        throw new IllegalArgumentException("Unbalanced JSON object in LLM response");
    }
}