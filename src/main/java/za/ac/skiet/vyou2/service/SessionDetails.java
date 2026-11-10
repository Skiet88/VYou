package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.domain.InterviewTurn;

import java.util.List;

public record SessionDetails(InterviewSession session, List<InterviewTurn> turns) {
}
