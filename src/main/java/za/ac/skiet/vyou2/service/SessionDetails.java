package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewSummary;
import za.ac.skiet.vyou2.domain.InterviewTurn;

import java.util.List;
import java.util.Optional;

public record SessionDetails(InterviewSession session, List<InterviewTurn> turns, Optional<InterviewSummary> summary) {
}
