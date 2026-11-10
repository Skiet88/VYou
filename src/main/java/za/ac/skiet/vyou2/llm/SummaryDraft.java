package za.ac.skiet.vyou2.llm;

import java.util.List;

/** Narrative part of a summary as produced by the LLM; scores are averaged locally. */
public record SummaryDraft(String feedback, List<String> topStrengths, List<String> focusAreas) {
}
