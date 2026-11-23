package com.finance.planner.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** Redis-backed, cross-instance login throttling with an atomic fixed window. */
@Service
public class RedisLoginAttemptService implements LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 15 * 60;
    private static final String KEY_PREFIX = "planwise:security:login-failures:";
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 or redis.call('TTL', KEYS[1]) < 0 then "
                    + "redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisLoginAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void ensureAllowed(String key) {
        String count = redis.opsForValue().get(redisKey(key));
        if (count != null && Long.parseLong(count) >= MAX_FAILED_ATTEMPTS) {
            throw new TooManyLoginAttemptsException();
        }
    }

    @Override
    public void recordFailure(String key) {
        Long count = redis.execute(INCREMENT_WITH_TTL, java.util.List.of(redisKey(key)),
                Long.toString(WINDOW_SECONDS));
        if (count == null) {
            throw new IllegalStateException("Redis did not return the login failure counter");
        }
    }

    @Override
    public void recordSuccess(String key) {
        redis.delete(redisKey(key));
    }

    private String redisKey(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
