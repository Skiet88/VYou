package za.ac.skiet.vyou2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Two buckets: "auth" protects login/signup from credential-stuffing and account-creation abuse
 * (keyed by client IP, since the caller isn't authenticated yet); "ai" protects the endpoints that
 * call the LLM from burning through the free daily quota (keyed by user id).
 */
@ConfigurationProperties(prefix = "vyou.ratelimit")
public record RateLimitProperties(Bucket auth, Bucket ai) {

    public RateLimitProperties {
        auth = auth == null ? new Bucket(null, null) : auth;
        ai = ai == null ? new Bucket(null, null) : ai;
    }

    public record Bucket(Integer capacity, Duration refillPeriod) {
        public Bucket {
            capacity = capacity == null ? 10 : capacity;
            refillPeriod = refillPeriod == null ? Duration.ofMinutes(1) : refillPeriod;
        }
    }
}
