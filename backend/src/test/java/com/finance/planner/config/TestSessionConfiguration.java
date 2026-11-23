package com.finance.planner.config;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.MapSession;
import com.finance.planner.auth.InMemoryLoginAttemptService;
import com.finance.planner.auth.LoginAttemptService;
import java.time.Clock;

/** Uses an in-memory session repository in H2-backed tests; production uses Redis. */
@TestConfiguration(proxyBeanMethods = false)
@EnableSpringHttpSession
public class TestSessionConfiguration {

    @Bean
    SessionRepository<MapSession> sessionRepository() {
        return new MapSessionRepository(new ConcurrentHashMap<>());
    }

    @Bean
    @Primary
    LoginAttemptService loginAttemptService() {
        return new InMemoryLoginAttemptService(Clock.systemUTC());
    }
}
