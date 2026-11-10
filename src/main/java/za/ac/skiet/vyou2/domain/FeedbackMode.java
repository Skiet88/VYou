package za.ac.skiet.vyou2.domain;

public enum FeedbackMode {
    /** Scores and feedback are returned after every answer. */
    INSTANT,
    /** Scores are computed and stored immediately but withheld from the API until the interview is completed. */
    END_OF_INTERVIEW
}
