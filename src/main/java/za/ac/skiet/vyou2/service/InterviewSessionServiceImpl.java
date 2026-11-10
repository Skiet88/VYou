package za.ac.skiet.vyou2.service;

import za.ac.skiet.vyou2.domain.InterviewSession;
import za.ac.skiet.vyou2.exception.ResourceNotFoundException;
import za.ac.skiet.vyou2.repository.InterviewSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.ac.skiet.vyou2.domain.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionRepository repository;

    public InterviewSessionServiceImpl(InterviewSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public InterviewSession save(InterviewSession session) {
        return repository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSession get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InterviewSession> forUser(UUID userId, InterviewStatus status, int page, int size) {
        // Newest first, with id as a tie-breaker so paging is stable when start times are equal.
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id")));
        Page<InterviewSession> result = status == null
                ? repository.findByUserId(userId, request)
                : repository.findByUserIdAndStatus(userId, status, request);
        return new PageResult<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
