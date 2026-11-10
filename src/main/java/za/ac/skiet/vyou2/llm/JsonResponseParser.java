package za.ac.skiet.vyou2.llm;

import za.ac.skiet.vyou2.exception.LlmResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Parses JSON model output, tolerating markdown code fences. */
final class JsonResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonResponseParser() {
    }

    static <T> T parse(String raw, Class<T> type) {
        try {
            return MAPPER.readValue(stripCodeFence(raw), type);
        } catch (JsonProcessingException ex) {
            throw new LlmResponseException("Model output was not valid JSON for " + type.getSimpleName(), ex);
        }
    }

    private static String stripCodeFence(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return s;
    }
}
