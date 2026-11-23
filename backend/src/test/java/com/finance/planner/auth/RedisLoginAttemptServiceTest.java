package com.finance.planner.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class RedisLoginAttemptServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private RedisLoginAttemptService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        values = org.mockito.Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new RedisLoginAttemptService(redis);
    }

    @Test
    void blocksTheSixthAttemptAndNeverUsesRawEmailAsRedisKey() {
        when(values.get(any())).thenReturn("5");

        assertThrows(TooManyLoginAttemptsException.class,
                () -> service.ensureAllowed("127.0.0.1:private@example.com"));

        org.mockito.ArgumentCaptor<String> key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(values).get(key.capture());
        org.junit.jupiter.api.Assertions.assertTrue(key.getValue().startsWith("planwise:security:login-failures:"));
        org.junit.jupiter.api.Assertions.assertFalse(key.getValue().contains("private@example.com"));
    }

    @Test
    void incrementsAtomicallyWithFifteenMinuteExpiry() {
        when(redis.execute(any(RedisScript.class), any(List.class), eq("900"))).thenReturn(1L);

        service.recordFailure("127.0.0.1:owner@example.com");

        verify(redis).execute(any(RedisScript.class), any(List.class), eq("900"));
    }

    @Test
    void successfulLoginDeletesOnlyItsFailureWindow() {
        service.recordSuccess("127.0.0.1:owner@example.com");

        verify(redis).delete(any(String.class));
    }
}
