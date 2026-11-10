package za.ac.skiet.vyou2.llm;

import java.util.List;

/**
 * Strategy for talking to a chat-completion model. The default implementation targets
 * any OpenAI-compatible endpoint (GitHub Models GPT-4o by default; Groq or OpenRouter by config).
 */
public interface LlmClient {

    /** Returns the assistant message content. When {@code jsonMode} is true the model is asked for a JSON object. */
    String complete(List<ChatMessage> messages, boolean jsonMode);
}
