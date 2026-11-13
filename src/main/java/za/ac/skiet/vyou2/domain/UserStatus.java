package za.ac.skiet.vyou2.domain;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    /** The user asked for their account to be deleted; it is purged after the grace period unless they sign in again. */
    PENDING_DELETION
}
