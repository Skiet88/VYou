package za.ac.skiet.vyou2.schemas;

import java.util.List;

public record SessionDetailsResponse(SessionResponse session, List<TurnResponse> turns) {
}
