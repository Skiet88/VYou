package za.ac.skiet.vyou2.schemas;

import java.time.Instant;
import java.util.List;

public record SummaryResponse(int questionsAnswered, double averageClarity, double averageTechnical,
                              double averageConfidence, double averageOverall, String feedback,
                              List<String> topStrengths, List<String> focusAreas, Instant createdAt) {
}
