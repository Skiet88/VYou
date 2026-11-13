package za.ac.skiet.vyou2.schemas;

/** {@code accountReactivated} is true when this login cancelled a scheduled account deletion. */
public record LoginResponse(UserResponse user, boolean accountReactivated) {
}
