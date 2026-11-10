package za.ac.skiet.vyou2.schemas;

/** {@code summary} is null if no questions were answered or the AI provider was unavailable. */
public record CompletionResponse(SessionResponse session, SummaryResponse summary) {
}
