package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;

import java.util.UUID;

public interface InterviewSessionService {

    InterviewSession save(InterviewSession session);

    /** @throws za.ac.skiet.vyou2.exception.ResourceNotFoundException if no session has this id */
    InterviewSession get(UUID id);
}
